// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.HttpClientFactory;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;
import software.aws.toolkits.eclipse.amazonq.util.ThreadingUtils;

/**
 * Polls the CDN for Eclipse notification payloads at a fixed interval.
 *
 * <p>Mirrors the JetBrains {@code NotificationPollingService} pattern: polls every
 * 10 minutes on a background thread, uses HTTP ETag headers to skip unchanged
 * payloads, retries up to 3 times with exponential backoff on failure, and
 * notifies registered observers when new notifications are available.</p>
 *
 * <p>Uses {@link HttpClientFactory#getInstance()} for proxy-aware HTTP,
 * {@link ObjectMapperFactory#getInstance()} for JSON deserialization, and
 * {@link ThreadingUtils} for background scheduling.</p>
 */
public final class NotificationPollingService {

    static final String DEFAULT_ENDPOINT =
            "https://idetoolkits-hostedfiles.amazonaws.com/Notifications/Eclipse/combined/2.x.json";

    private static final long POLLING_INTERVAL_MS = Duration.ofMinutes(10).toMillis();
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000L;
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_MODIFIED = 304;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private final HttpClient httpClient;
    private final String endpoint;
    private final List<Runnable> observers = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> cachedETag = new AtomicReference<>();
    private final AtomicReference<NotificationsList> cachedNotifications = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile Future<?> scheduledFuture;

    /**
     * Creates a polling service with the default CDN endpoint.
     */
    public NotificationPollingService() {
        this(DEFAULT_ENDPOINT, HttpClientFactory.getInstance());
    }

    /**
     * Creates a polling service with a custom endpoint and HTTP client.
     * Visible for testing.
     */
    NotificationPollingService(final String endpoint, final HttpClient httpClient) {
        this.endpoint = endpoint;
        this.httpClient = httpClient;
    }

    /**
     * Starts periodic polling on a background thread. If already running, this
     * method is a no-op.
     */
    public void startPolling() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        scheduleNextPoll(0);
    }

    /**
     * Stops polling and cancels any pending scheduled poll. Safe to call
     * multiple times.
     */
    public void stopPolling() {
        running.set(false);
        Future<?> future = scheduledFuture;
        if (future != null) {
            future.cancel(false);
            scheduledFuture = null;
        }
    }

    /**
     * Registers an observer that is notified when new notifications are fetched.
     *
     * @param observer the callback to invoke on new notifications
     */
    public void addObserver(final Runnable observer) {
        observers.add(observer);
    }

    /**
     * Returns the most recently fetched notifications, or {@code null} if none
     * have been fetched yet.
     */
    public NotificationsList getCachedNotifications() {
        return cachedNotifications.get();
    }

    /**
     * Schedules the next poll after the given delay in milliseconds.
     */
    private void scheduleNextPoll(final long delayMs) {
        if (!running.get()) {
            return;
        }
        scheduledFuture = ThreadingUtils.scheduleAsyncTaskWithDelay(this::pollAndReschedule, delayMs);
    }

    /**
     * Executes a single poll cycle and schedules the next one.
     */
    private void pollAndReschedule() {
        try {
            boolean hasNewData = pollWithRetries();
            if (hasNewData) {
                notifyObservers();
            }
        } catch (Exception e) {
            Activator.getLogger().error("Unexpected error during notification polling cycle", e);
        } finally {
            scheduleNextPoll(POLLING_INTERVAL_MS);
        }
    }

    /**
     * Attempts to fetch notifications from the CDN, retrying up to
     * {@value #MAX_RETRIES} times with exponential backoff on failure.
     *
     * @return {@code true} if new notifications were fetched and cached
     */
    private boolean pollWithRetries() {
        Exception lastException = null;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return poll();
            } catch (Exception e) {
                lastException = e;
                Activator.getLogger().warn(
                        String.format("Failed to poll for notifications (attempt %d/%d): %s",
                                attempt + 1, MAX_RETRIES, e.getMessage()), e);

                if (attempt < MAX_RETRIES - 1) {
                    long backoffMs = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                    sleep(backoffMs);
                }
            }
        }

        Activator.getLogger().error(
                "All notification polling retries exhausted. Will retry at next scheduled interval.",
                lastException);
        return false;
    }

    /**
     * Performs a single HTTP request to the CDN endpoint.
     *
     * @return {@code true} if new notifications were received and cached
     * @throws Exception if the request fails
     */
    private boolean poll() throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .GET();

        String etag = cachedETag.get();
        if (etag != null) {
            requestBuilder.header("If-None-Match", etag);
        }

        HttpResponse<String> response = httpClient.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString());

        int statusCode = response.statusCode();

        if (statusCode == HTTP_NOT_MODIFIED) {
            Activator.getLogger().info("Notification payload not modified (HTTP 304), skipping processing");
            return false;
        }

        if (statusCode == HTTP_OK) {
            NotificationsList notifications = OBJECT_MAPPER.readValue(
                    response.body(), NotificationsList.class);

            // Cache the new ETag
            response.headers().firstValue("ETag").ifPresent(cachedETag::set);

            // Cache the deserialized notifications
            cachedNotifications.set(notifications);

            Activator.getLogger().info("Fetched new notification payload from CDN");
            return true;
        }

        // Non-200/304 status — treat as error
        throw new NotificationPollingException(
                "Unexpected HTTP status code from notification endpoint: " + statusCode);
    }

    /**
     * Notifies all registered observers that new notifications are available.
     */
    private void notifyObservers() {
        for (Runnable observer : observers) {
            try {
                observer.run();
            } catch (Exception e) {
                Activator.getLogger().warn("Notification observer threw an exception", e);
            }
        }
    }

    /**
     * Sleeps for the specified duration. Extracted for testability.
     */
    void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Exception indicating a polling failure due to an unexpected HTTP response.
     */
    static class NotificationPollingException extends Exception {
        private static final long serialVersionUID = 1L;

        NotificationPollingException(final String message) {
            super(message);
        }
    }
}
