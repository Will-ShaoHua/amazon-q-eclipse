// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;

/**
 * Property-based tests for {@link NotificationDismissalManager} dismissal round-trip.
 *
 * <p>Uses an in-memory {@link PluginStore} implementation to test the real
 * serialization/deserialization round-trip without mocking.</p>
 *
 * <p>Validates: Requirements 8.1, 8.3</p>
 */
@Tag("Feature:maintenance-mode-notification")
public final class NotificationDismissalPropertyTest {

    /**
     * Property 4: Dismissal round-trip.
     *
     * <p>For any random notification ID string, after calling {@code dismiss(id)},
     * the notification should be excluded from the filtered notification list — that is,
     * {@code isDismissed(id)} returns true and a list of notifications containing that ID,
     * when filtered against the dismissal store, should not include it.</p>
     *
     * <p><b>Validates: Requirements 8.1, 8.3</b></p>
     */
    @Property(tries = 100)
    @Tag("Property4:Dismissal-round-trip")
    void dismissedNotificationIsExcludedFromFilteredList(
            @ForAll("notificationIdLists") final List<String> allIds,
            @ForAll("notificationIds") final String dismissId) {

        InMemoryPluginStore store = new InMemoryPluginStore();
        NotificationDismissalManager manager = new NotificationDismissalManager(store);

        // Dismiss the target ID
        manager.dismiss(dismissId);

        // Verify the dismissed ID is reported as dismissed
        assertTrue(manager.isDismissed(dismissId),
                String.format("dismiss(%s) should make isDismissed return true", dismissId));

        // Simulate filtering a notification list against the dismissal store
        List<String> combinedIds = new ArrayList<>(allIds);
        if (!combinedIds.contains(dismissId)) {
            combinedIds.add(dismissId);
        }

        List<String> filteredIds = combinedIds.stream()
                .filter(id -> !manager.isDismissed(id))
                .collect(Collectors.toList());

        // The dismissed ID must not appear in the filtered list
        assertFalse(filteredIds.contains(dismissId),
                String.format("Dismissed ID '%s' should not appear in filtered list", dismissId));

        // All non-dismissed IDs should still be present
        Set<String> nonDismissedIds = combinedIds.stream()
                .filter(id -> !id.equals(dismissId))
                .collect(Collectors.toSet());
        for (String id : nonDismissedIds) {
            assertTrue(filteredIds.contains(id),
                    String.format("Non-dismissed ID '%s' should remain in filtered list", id));
        }
    }

    /**
     * Property 5: Stale dismissal cleanup.
     *
     * <p>For any set of dismissed notification IDs and any set of active notification IDs,
     * after calling {@code cleanupStaleEntries(activeIds)}, the dismissal store should
     * contain only IDs that are in both the original dismissed set and the active set
     * (i.e., the intersection).</p>
     *
     * <p><b>Validates: Requirements 8.4</b></p>
     */
    @Property(tries = 100)
    @Tag("Property5:Stale-dismissal-cleanup")
    void cleanupStaleEntriesRetainsOnlyIntersection(
            @ForAll("notificationIdSets") final Set<String> dismissedIds,
            @ForAll("notificationIdSets") final Set<String> activeIds) {

        InMemoryPluginStore store = new InMemoryPluginStore();
        NotificationDismissalManager manager = new NotificationDismissalManager(store);

        // Dismiss all IDs in the dismissed set
        for (String id : dismissedIds) {
            manager.dismiss(id);
        }

        // Verify all dismissed IDs are reported as dismissed before cleanup
        for (String id : dismissedIds) {
            assertTrue(manager.isDismissed(id),
                    String.format("ID '%s' should be dismissed before cleanup", id));
        }

        // Perform stale entry cleanup
        manager.cleanupStaleEntries(activeIds);

        // Compute expected intersection: IDs in both dismissed and active sets
        Set<String> expectedRetained = new HashSet<>(dismissedIds);
        expectedRetained.retainAll(activeIds);

        // IDs in the intersection should still be dismissed
        for (String id : expectedRetained) {
            assertTrue(manager.isDismissed(id),
                    String.format("ID '%s' is in both dismissed and active sets, should still be dismissed after cleanup", id));
        }

        // IDs that were dismissed but are NOT in the active set should be removed
        Set<String> expectedRemoved = new HashSet<>(dismissedIds);
        expectedRemoved.removeAll(activeIds);
        for (String id : expectedRemoved) {
            assertFalse(manager.isDismissed(id),
                    String.format("ID '%s' was dismissed but not active, should be removed after cleanup", id));
        }

        // IDs that were never dismissed should remain not dismissed
        Set<String> neverDismissed = new HashSet<>(activeIds);
        neverDismissed.removeAll(dismissedIds);
        for (String id : neverDismissed) {
            assertFalse(manager.isDismissed(id),
                    String.format("ID '%s' was never dismissed, should not be dismissed after cleanup", id));
        }
    }

    // ---- Generators ----

    @Provide
    Arbitrary<String> notificationIds() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('-', '_', '.')
                .ofMinLength(1)
                .ofMaxLength(40);
    }

    @Provide
    Arbitrary<List<String>> notificationIdLists() {
        return notificationIds().list().ofMinSize(0).ofMaxSize(10);
    }

    @Provide
    Arbitrary<Set<String>> notificationIdSets() {
        return notificationIds().set().ofMinSize(0).ofMaxSize(10);
    }

    // ---- In-memory PluginStore for property testing ----

    /**
     * Simple in-memory {@link PluginStore} implementation that avoids
     * Eclipse runtime dependencies, enabling property tests to exercise
     * the real serialization/deserialization round-trip in
     * {@link NotificationDismissalManager}.
     */
    static final class InMemoryPluginStore implements PluginStore {
        private final Map<String, String> data = new HashMap<>();

        @Override
        public void put(final String key, final String value) {
            data.put(key, value);
        }

        @Override
        public String get(final String key) {
            return data.get(key);
        }

        @Override
        public void remove(final String key) {
            data.remove(key);
        }

        @Override
        public void addChangeListener(final IPreferenceChangeListener prefChangeListener) {
            // no-op for testing
        }

        @Override
        public <T> void putObject(final String key, final T value) {
            // no-op for testing
        }

        @Override
        public <T> T getObject(final String key, final Class<T> type) {
            return null;
        }
    }
}
