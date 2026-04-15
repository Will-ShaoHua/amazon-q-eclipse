// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import software.aws.toolkits.eclipse.amazonq.configuration.PluginStore;
import software.aws.toolkits.eclipse.amazonq.plugin.Activator;
import software.aws.toolkits.eclipse.amazonq.util.LoggingService;

/**
 * Unit tests for {@link NotificationDismissalManager}.
 *
 * <p>Tests cover dismiss/isDismissed round-trip, stale entry cleanup,
 * empty state handling, and corrupted store recovery.</p>
 *
 * <p>Validates: Requirements 8.1, 8.2, 8.3, 8.4</p>
 */
public final class NotificationDismissalManagerTest {

    @Mock
    private PluginStore pluginStore;

    private NotificationDismissalManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        manager = new NotificationDismissalManager(pluginStore);
    }

    // ---- Requirement 8.1: Dismiss persists notification ID ----

    @Nested
    class DismissTests {
        @Test
        void dismissPersistsNotificationId() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(null);

            manager.dismiss("notif-001");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            assertTrue(captor.getValue().contains("notif-001"));
        }

        @Test
        void dismissAddsToExistingDismissedIds() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\"]");

            manager.dismiss("notif-002");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            String stored = captor.getValue();
            assertTrue(stored.contains("notif-001"));
            assertTrue(stored.contains("notif-002"));
        }

        @Test
        void dismissDuplicateIdIsIdempotent() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\"]");

            manager.dismiss("notif-001");

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            assertTrue(captor.getValue().contains("notif-001"));
        }
    }

    // ---- Requirement 8.3: isDismissed filters dismissed notifications ----

    @Nested
    class IsDismissedTests {
        @Test
        void isDismissedReturnsTrueForDismissedId() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\",\"notif-002\"]");

            assertTrue(manager.isDismissed("notif-001"));
            assertTrue(manager.isDismissed("notif-002"));
        }

        @Test
        void isDismissedReturnsFalseForNonDismissedId() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\"]");

            assertFalse(manager.isDismissed("notif-999"));
        }

        @Test
        void isDismissedReturnsFalseWhenStoreIsEmpty() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(null);

            assertFalse(manager.isDismissed("notif-001"));
        }

        @Test
        void isDismissedReturnsFalseWhenStoreIsBlank() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn("   ");

            assertFalse(manager.isDismissed("notif-001"));
        }
    }

    // ---- Requirements 8.1, 8.3: Dismiss/isDismissed round-trip ----

    @Nested
    class DismissRoundTripTests {
        @Test
        void dismissThenIsDismissedRoundTrip() {
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(null);

            manager.dismiss("notif-abc");

            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            String storedJson = captor.getValue();

            // Simulate the store returning what was persisted
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(storedJson);

            assertTrue(manager.isDismissed("notif-abc"));
            assertFalse(manager.isDismissed("notif-other"));
        }

        @Test
        void dismissMultipleThenVerifyAll() {
            // First dismiss
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(null);
            manager.dismiss("id-1");

            ArgumentCaptor<String> captor1 = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor1.capture());
            String afterFirst = captor1.getValue();

            // Second dismiss
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(afterFirst);
            manager.dismiss("id-2");

            ArgumentCaptor<String> captor2 = ArgumentCaptor.forClass(String.class);
            verify(pluginStore, times(2))
                    .put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor2.capture());
            String afterSecond = captor2.getAllValues().get(1);

            // Verify both are dismissed
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(afterSecond);
            assertTrue(manager.isDismissed("id-1"));
            assertTrue(manager.isDismissed("id-2"));
            assertFalse(manager.isDismissed("id-3"));
        }
    }

    // ---- Requirement 8.4: Stale entry cleanup ----

    @Nested
    class CleanupStaleEntriesTests {
        @Test
        void cleanupRetainsOnlyActiveIds() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\",\"notif-002\",\"notif-003\"]");

            manager.cleanupStaleEntries(Set.of("notif-001", "notif-003", "notif-004"));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            String stored = captor.getValue();
            assertTrue(stored.contains("notif-001"));
            assertFalse(stored.contains("notif-002"));
            assertTrue(stored.contains("notif-003"));
            assertFalse(stored.contains("notif-004"));
        }

        @Test
        void cleanupRemovesAllWhenNoActiveIdsMatch() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\",\"notif-002\"]");

            manager.cleanupStaleEntries(Set.of("notif-999"));

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            String stored = captor.getValue();
            assertFalse(stored.contains("notif-001"));
            assertFalse(stored.contains("notif-002"));
        }

        @Test
        void cleanupDoesNothingWhenDismissedSetIsEmpty() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn(null);

            manager.cleanupStaleEntries(Set.of("notif-001"));

            verify(pluginStore, never()).put(
                    eq(NotificationDismissalManager.DISMISSED_IDS_KEY),
                    ArgumentMatchers.anyString());
        }

        @Test
        void cleanupDoesNothingWhenAllDismissedAreActive() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\",\"notif-002\"]");

            manager.cleanupStaleEntries(Set.of("notif-001", "notif-002", "notif-003"));

            verify(pluginStore, never()).put(
                    eq(NotificationDismissalManager.DISMISSED_IDS_KEY),
                    ArgumentMatchers.anyString());
        }

        @Test
        void cleanupWithEmptyActiveSetRemovesAll() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                    .thenReturn("[\"notif-001\"]");

            manager.cleanupStaleEntries(Set.of());

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
            String stored = captor.getValue();
            assertFalse(stored.contains("notif-001"));
        }
    }

    // ---- Requirement 8.2: Store uses PluginStore ----

    @Nested
    class EmptyAndCorruptStateTests {
        @Test
        void emptyJsonArrayReturnsNoDismissals() {
            when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY)).thenReturn("[]");

            assertFalse(manager.isDismissed("notif-001"));
        }

        @Test
        void corruptedJsonTreatedAsEmptyStore() {
            try (MockedStatic<Activator> activatorMock = mockStatic(Activator.class)) {
                LoggingService loggerMock = mock(LoggingService.class);
                activatorMock.when(Activator::getLogger).thenReturn(loggerMock);

                when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                        .thenReturn("{not valid json!!!");

                assertFalse(manager.isDismissed("notif-001"));
            }
        }

        @Test
        void dismissAfterCorruptedStoreStartsFresh() {
            try (MockedStatic<Activator> activatorMock = mockStatic(Activator.class)) {
                LoggingService loggerMock = mock(LoggingService.class);
                activatorMock.when(Activator::getLogger).thenReturn(loggerMock);

                when(pluginStore.get(NotificationDismissalManager.DISMISSED_IDS_KEY))
                        .thenReturn("{not valid json!!!");

                manager.dismiss("notif-new");

                ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
                verify(pluginStore).put(eq(NotificationDismissalManager.DISMISSED_IDS_KEY), captor.capture());
                String stored = captor.getValue();
                assertTrue(stored.contains("notif-new"));
            }
        }
    }
}
