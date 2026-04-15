// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.ObjectMapperFactory;

/**
 * Manages notification dismissal state, persisting dismissed notification IDs
 * to the Eclipse plugin preferences via {@link PluginStore}.
 *
 * <p>Dismissed IDs are stored as a JSON-serialized {@code Set<String>} under the
 * key {@value #DISMISSED_IDS_KEY}. All public methods are synchronized to ensure
 * thread-safe access to the underlying store.</p>
 *
 * <p>Call {@link #cleanupStaleEntries(Set)} each polling cycle to remove IDs that
 * are no longer present in the CDN payload, preventing unbounded growth of the
 * dismissal store.</p>
 */
public final class NotificationDismissalManager {

    static final String DISMISSED_IDS_KEY = "notification.dismissed.ids";

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private final PluginStore pluginStore;

    public NotificationDismissalManager(final PluginStore pluginStore) {
        this.pluginStore = pluginStore;
    }

    public NotificationDismissalManager() {
        this(Activator.getPluginStore());
    }

    /**
     * Returns {@code true} if the given notification ID has been dismissed.
     */
    public synchronized boolean isDismissed(final String notificationId) {
        return loadDismissedIds().contains(notificationId);
    }

    /**
     * Persists the given notification ID as dismissed.
     */
    public synchronized void dismiss(final String notificationId) {
        Set<String> dismissedIds = loadDismissedIds();
        dismissedIds.add(notificationId);
        saveDismissedIds(dismissedIds);
    }

    /**
     * Removes dismissed IDs that are no longer present in the CDN payload.
     *
     * <p>After this call, only IDs that appear in both the current dismissal store
     * and the provided {@code activeNotificationIds} set are retained.</p>
     *
     * @param activeNotificationIds the set of notification IDs currently in the CDN payload
     */
    public synchronized void cleanupStaleEntries(final Set<String> activeNotificationIds) {
        Set<String> dismissedIds = loadDismissedIds();
        if (dismissedIds.isEmpty()) {
            return;
        }
        boolean changed = dismissedIds.retainAll(activeNotificationIds);
        if (changed) {
            saveDismissedIds(dismissedIds);
        }
    }

    private Set<String> loadDismissedIds() {
        String json = pluginStore.get(DISMISSED_IDS_KEY);
        if (json == null || json.isBlank()) {
            return new HashSet<>();
        }
        try {
            Set<String> ids = OBJECT_MAPPER.readValue(json, new TypeReference<Set<String>>() { });
            return new HashSet<>(ids);
        } catch (JsonProcessingException e) {
            Activator.getLogger().warn("Failed to deserialize dismissed notification IDs, resetting store", e);
            return new HashSet<>();
        }
    }

    private void saveDismissedIds(final Set<String> dismissedIds) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(dismissedIds);
            pluginStore.put(DISMISSED_IDS_KEY, json);
        } catch (JsonProcessingException e) {
            Activator.getLogger().warn("Failed to serialize dismissed notification IDs", e);
        }
    }
}
