// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

/**
 * Orchestrates the notification subsystem by wiring together the polling service,
 * rules engine, UI layer, and dismissal manager.
 *
 * <p>When the polling service fetches new notifications, this processor:</p>
 * <ol>
 *   <li>Collects all active notification IDs and cleans up stale dismissal entries</li>
 *   <li>Filters out dismissed notifications</li>
 *   <li>Evaluates each remaining notification through the {@link RulesEngine}</li>
 *   <li>Displays passing notifications via {@link NotificationUI}</li>
 *   <li>Wires the UI dismissal callback back to the {@link NotificationDismissalManager}</li>
 * </ol>
 *
 * <p>Lifecycle is managed via {@link #start()} and {@link #stop()}, which delegate
 * to the underlying {@link NotificationPollingService}.</p>
 */
public final class NotificationProcessor {

    private final NotificationPollingService pollingService;
    private final NotificationDismissalManager dismissalManager;

    /**
     * Creates a processor with default dependencies.
     */
    public NotificationProcessor() {
        this(new NotificationPollingService(), new NotificationDismissalManager());
    }

    /**
     * Creates a processor with the given dependencies. Visible for testing.
     */
    NotificationProcessor(final NotificationPollingService pollingService,
                          final NotificationDismissalManager dismissalManager) {
        this.pollingService = pollingService;
        this.dismissalManager = dismissalManager;
        this.pollingService.addObserver(this::onNewNotifications);
    }

    /**
     * Starts the notification polling service.
     */
    public void start() {
        pollingService.startPolling();
    }

    /**
     * Stops the notification polling service and releases resources.
     */
    public void stop() {
        pollingService.stopPolling();
    }

    /**
     * Observer callback invoked when the polling service fetches new notifications.
     * Processes each notification through the dismissal filter, rules engine, and UI.
     */
    private void onNewNotifications() {
        try {
            NotificationsList notificationsList = pollingService.getCachedNotifications();
            if (notificationsList == null || notificationsList.getNotifications() == null) {
                return;
            }

            List<NotificationData> notifications = notificationsList.getNotifications();

            // Collect all active notification IDs for stale entry cleanup
            Set<String> activeIds = new HashSet<>();
            for (NotificationData notification : notifications) {
                if (notification.getId() != null) {
                    activeIds.add(notification.getId());
                }
            }

            // Clean up dismissed IDs that are no longer in the CDN payload
            dismissalManager.cleanupStaleEntries(activeIds);

            // Process each notification through the filter pipeline
            for (NotificationData notification : notifications) {
                processNotification(notification);
            }
        } catch (Exception e) {
            Activator.getLogger().error("Error processing notifications", e);
        }
    }

    /**
     * Processes a single notification: checks dismissal state, evaluates rules,
     * and displays if all checks pass.
     */
    private void processNotification(final NotificationData notification) {
        if (notification.getId() == null) {
            return;
        }

        // Skip dismissed notifications
        if (dismissalManager.isDismissed(notification.getId())) {
            return;
        }

        // Evaluate display conditions
        if (!RulesEngine.shouldDisplay(notification)) {
            return;
        }

        // Display the notification with a dismissal callback
        NotificationUI.showNotification(notification, dismissalManager::dismiss);
    }
}
