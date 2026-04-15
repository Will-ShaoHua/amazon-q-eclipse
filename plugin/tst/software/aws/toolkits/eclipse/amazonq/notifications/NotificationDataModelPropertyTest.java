// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for the notification data model deserialization round-trip.
 *
 * <p>Validates: Requirements 4.3, 5.1, 5.2, 5.3, 5.4, 5.5</p>
 */
@Tag("Feature:maintenance-mode-notification")
@Tag("Property1:Notification-deserialization-round-trip")
public final class NotificationDataModelPropertyTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Property 1: Notification deserialization round-trip.
     *
     * <p>For any valid NotificationsList Java object, serializing it to JSON
     * and deserializing it back should produce an object with equivalent field values.</p>
     *
     * <p><b>Validates: Requirements 4.3, 5.1, 5.2, 5.3, 5.4, 5.5</b></p>
     */
    @Property(tries = 100)
    void notificationsListRoundTrip(
            @ForAll("notificationsLists") final NotificationsList original) throws Exception {
        String json = OBJECT_MAPPER.writeValueAsString(original);
        NotificationsList deserialized = OBJECT_MAPPER.readValue(json, NotificationsList.class);

        assertNotNull(deserialized);
        assertSchemaEquals(original.getSchema(), deserialized.getSchema());
        assertNotificationsEquals(original.getNotifications(), deserialized.getNotifications());
    }

    @Provide
    Arbitrary<NotificationsList> notificationsLists() {
        return Combinators.combine(schemas(), notificationDataLists())
                .as(NotificationsList::new);
    }

    @Provide
    Arbitrary<Schema> schemas() {
        return safeStrings().map(Schema::new);
    }

    @Provide
    Arbitrary<List<NotificationData>> notificationDataLists() {
        return notificationDataItems().list().ofMinSize(0).ofMaxSize(3);
    }

    @Provide
    Arbitrary<NotificationData> notificationDataItems() {
        return Combinators.combine(
                safeStrings(),
                schedules(),
                severities(),
                conditions(),
                contentLocales(),
                actionLists()
        ).as(NotificationData::new);
    }

    @Provide
    Arbitrary<NotificationSchedule> schedules() {
        return Arbitraries.of("Emergency", "Startup").map(NotificationSchedule::new);
    }

    @Provide
    Arbitrary<String> severities() {
        return Arbitraries.of("Critical", "Warning", "Info");
    }

    @Provide
    Arbitrary<NotificationDisplayCondition> conditions() {
        return Combinators.combine(
                computeTypes().injectNull(0.3),
                systemTypes().injectNull(0.3),
                systemTypes().injectNull(0.3),
                extensionTypeLists().injectNull(0.3),
                authxTypeLists().injectNull(0.3)
        ).as(NotificationDisplayCondition::new);
    }

    @Provide
    Arbitrary<ComputeType> computeTypes() {
        return Combinators.combine(
                leafExpressions().injectNull(0.3),
                leafExpressions().injectNull(0.3)
        ).as(ComputeType::new);
    }

    @Provide
    Arbitrary<SystemType> systemTypes() {
        return Combinators.combine(
                leafExpressions().injectNull(0.3),
                leafExpressions().injectNull(0.3)
        ).as(SystemType::new);
    }

    @Provide
    Arbitrary<List<ExtensionType>> extensionTypeLists() {
        return extensionTypes().list().ofMinSize(1).ofMaxSize(2);
    }

    @Provide
    Arbitrary<ExtensionType> extensionTypes() {
        return Combinators.combine(
                safeStrings(),
                leafExpressions().injectNull(0.3)
        ).as(ExtensionType::new);
    }

    @Provide
    Arbitrary<List<AuthxType>> authxTypeLists() {
        return authxTypes().list().ofMinSize(1).ofMaxSize(2);
    }

    @Provide
    Arbitrary<AuthxType> authxTypes() {
        return Combinators.combine(
                safeStrings(),
                leafExpressions().injectNull(0.3),
                leafExpressions().injectNull(0.3),
                leafExpressions().injectNull(0.3),
                leafExpressions().injectNull(0.3)
        ).as(AuthxType::new);
    }

    @Provide
    Arbitrary<NotificationContentDescriptionLocale> contentLocales() {
        return contentDescriptions().map(NotificationContentDescriptionLocale::new);
    }

    @Provide
    Arbitrary<NotificationContentDescription> contentDescriptions() {
        return Combinators.combine(safeStrings(), safeStrings())
                .as(NotificationContentDescription::new);
    }

    @Provide
    Arbitrary<List<NotificationFollowupActions>> actionLists() {
        return followupActions().list().ofMinSize(0).ofMaxSize(2);
    }

    @Provide
    Arbitrary<NotificationFollowupActions> followupActions() {
        return Combinators.combine(
                Arbitraries.of("openUrl", "openSettings"),
                followupActionsContents()
        ).as(NotificationFollowupActions::new);
    }

    @Provide
    Arbitrary<NotificationFollowupActionsContent> followupActionsContents() {
        return actionDescriptions().map(NotificationFollowupActionsContent::new);
    }

    @Provide
    Arbitrary<NotificationActionDescription> actionDescriptions() {
        return Combinators.combine(safeStrings(), safeStrings())
                .as(NotificationActionDescription::new);
    }

    /**
     * Generates leaf-level NotificationExpression instances that support
     * symmetric serialization/deserialization (comparison and set operators).
     */
    @Provide
    Arbitrary<NotificationExpression> leafExpressions() {
        return Arbitraries.oneOf(
                safeStrings().map(EqExpression::new),
                safeStrings().map(NeqExpression::new),
                safeStrings().map(GtExpression::new),
                safeStrings().map(GteExpression::new),
                safeStrings().map(LtExpression::new),
                safeStrings().map(LteExpression::new),
                safeStrings().list().ofMinSize(1).ofMaxSize(4).map(AnyOfExpression::new),
                safeStrings().list().ofMinSize(1).ofMaxSize(4).map(NoneOfExpression::new)
        );
    }

    /**
     * Generates safe alphanumeric strings with dots and hyphens,
     * suitable for JSON serialization without escaping issues.
     */
    private Arbitrary<String> safeStrings() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('.', '-', '_')
                .ofMinLength(1)
                .ofMaxLength(30);
    }

    // --- Assertion helpers ---

    private void assertSchemaEquals(final Schema expected, final Schema actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getVersion(), actual.getVersion());
    }

    private void assertNotificationsEquals(
            final List<NotificationData> expected, final List<NotificationData> actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertNotificationDataEquals(expected.get(i), actual.get(i));
        }
    }

    private void assertNotificationDataEquals(
            final NotificationData expected, final NotificationData actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getSeverity(), actual.getSeverity());
        assertScheduleEquals(expected.getSchedule(), actual.getSchedule());
        assertConditionEquals(expected.getCondition(), actual.getCondition());
        assertContentLocaleEquals(expected.getContent(), actual.getContent());
        assertActionsEquals(expected.getActions(), actual.getActions());
    }

    private void assertScheduleEquals(
            final NotificationSchedule expected, final NotificationSchedule actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getType(), actual.getType());
    }

    private void assertConditionEquals(
            final NotificationDisplayCondition expected, final NotificationDisplayCondition actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertComputeTypeEquals(expected.getCompute(), actual.getCompute());
        assertSystemTypeEquals(expected.getOs(), actual.getOs());
        assertSystemTypeEquals(expected.getIde(), actual.getIde());
        assertExtensionListEquals(expected.getExtension(), actual.getExtension());
        assertAuthxListEquals(expected.getAuthx(), actual.getAuthx());
    }

    private void assertComputeTypeEquals(final ComputeType expected, final ComputeType actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertExpressionEquals(expected.getType(), actual.getType());
        assertExpressionEquals(expected.getArchitecture(), actual.getArchitecture());
    }

    private void assertSystemTypeEquals(final SystemType expected, final SystemType actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertExpressionEquals(expected.getType(), actual.getType());
        assertExpressionEquals(expected.getVersion(), actual.getVersion());
    }

    private void assertExtensionListEquals(
            final List<ExtensionType> expected, final List<ExtensionType> actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getId(), actual.get(i).getId());
            assertExpressionEquals(expected.get(i).getVersion(), actual.get(i).getVersion());
        }
    }

    private void assertAuthxListEquals(
            final List<AuthxType> expected, final List<AuthxType> actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            AuthxType exp = expected.get(i);
            AuthxType act = actual.get(i);
            assertEquals(exp.getFeature(), act.getFeature());
            assertExpressionEquals(exp.getType(), act.getType());
            assertExpressionEquals(exp.getRegion(), act.getRegion());
            assertExpressionEquals(exp.getConnectionState(), act.getConnectionState());
            assertExpressionEquals(exp.getSsoScopes(), act.getSsoScopes());
        }
    }

    private void assertContentLocaleEquals(
            final NotificationContentDescriptionLocale expected,
            final NotificationContentDescriptionLocale actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertContentDescriptionEquals(expected.getLocale(), actual.getLocale());
    }

    private void assertContentDescriptionEquals(
            final NotificationContentDescription expected,
            final NotificationContentDescription actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getDescription(), actual.getDescription());
    }

    private void assertActionsEquals(
            final List<NotificationFollowupActions> expected,
            final List<NotificationFollowupActions> actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getType(), actual.get(i).getType());
            assertFollowupContentEquals(expected.get(i).getContent(), actual.get(i).getContent());
        }
    }

    private void assertFollowupContentEquals(
            final NotificationFollowupActionsContent expected,
            final NotificationFollowupActionsContent actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertActionDescriptionEquals(expected.getLocale(), actual.getLocale());
    }

    private void assertActionDescriptionEquals(
            final NotificationActionDescription expected,
            final NotificationActionDescription actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getUrl(), actual.getUrl());
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private void assertExpressionEquals(
            final NotificationExpression expected, final NotificationExpression actual) {
        if (expected == null) {
            assertEquals(null, actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getClass(), actual.getClass());

        if (expected instanceof EqExpression) {
            assertEquals(((EqExpression) expected).getValue(), ((EqExpression) actual).getValue());
        } else if (expected instanceof NeqExpression) {
            assertEquals(((NeqExpression) expected).getValue(), ((NeqExpression) actual).getValue());
        } else if (expected instanceof GtExpression) {
            assertEquals(((GtExpression) expected).getValue(), ((GtExpression) actual).getValue());
        } else if (expected instanceof GteExpression) {
            assertEquals(((GteExpression) expected).getValue(), ((GteExpression) actual).getValue());
        } else if (expected instanceof LtExpression) {
            assertEquals(((LtExpression) expected).getValue(), ((LtExpression) actual).getValue());
        } else if (expected instanceof LteExpression) {
            assertEquals(((LteExpression) expected).getValue(), ((LteExpression) actual).getValue());
        } else if (expected instanceof AnyOfExpression) {
            assertEquals(((AnyOfExpression) expected).getValues(), ((AnyOfExpression) actual).getValues());
        } else if (expected instanceof NoneOfExpression) {
            assertEquals(((NoneOfExpression) expected).getValues(), ((NoneOfExpression) actual).getValues());
        }
    }
}
