// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RulesEngine}.
 *
 * <p>Tests cover expression evaluation for each operator type, semver comparison
 * edge cases, set membership, logical composition, AND logic for multiple conditions,
 * and null condition handling.</p>
 *
 * <p>Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7</p>
 */
public final class RulesEngineTest {

    // ---- Requirement 6.1: No condition → displayable ----

    @Test
    void shouldDisplayReturnsTrueWhenConditionIsNull() {
        NotificationData notification = new NotificationData();
        notification.setCondition(null);

        assertTrue(RulesEngine.shouldDisplay(notification));
    }

    // ---- Requirement 6.6: Comparison expressions with semver ----

    @Nested
    class EqExpressionTests {
        @Test
        void eqReturnsTrueForExactMatch() {
            assertTrue(RulesEngine.evaluateExpression(new EqExpression("Local"), "Local", false));
        }

        @Test
        void eqReturnsFalseForMismatch() {
            assertFalse(RulesEngine.evaluateExpression(new EqExpression("Local"), "Remote", false));
        }

        @Test
        void eqIsCaseSensitive() {
            assertFalse(RulesEngine.evaluateExpression(new EqExpression("local"), "Local", false));
        }
    }

    @Nested
    class NeqExpressionTests {
        @Test
        void neqReturnsTrueForMismatch() {
            assertTrue(RulesEngine.evaluateExpression(new NeqExpression("Remote"), "Local", false));
        }

        @Test
        void neqReturnsFalseForExactMatch() {
            assertFalse(RulesEngine.evaluateExpression(new NeqExpression("Local"), "Local", false));
        }
    }

    @Nested
    class GtExpressionTests {
        @Test
        void gtReturnsTrueWhenActualIsGreater() {
            assertTrue(RulesEngine.evaluateExpression(new GtExpression("1.0.0"), "1.0.1", true));
        }

        @Test
        void gtReturnsFalseWhenEqual() {
            assertFalse(RulesEngine.evaluateExpression(new GtExpression("1.0.0"), "1.0.0", true));
        }

        @Test
        void gtReturnsFalseWhenActualIsLess() {
            assertFalse(RulesEngine.evaluateExpression(new GtExpression("2.0.0"), "1.9.9", true));
        }
    }

    @Nested
    class GteExpressionTests {
        @Test
        void gteReturnsTrueWhenActualIsGreater() {
            assertTrue(RulesEngine.evaluateExpression(new GteExpression("1.0.0"), "1.0.1", true));
        }

        @Test
        void gteReturnsTrueWhenEqual() {
            assertTrue(RulesEngine.evaluateExpression(new GteExpression("1.0.0"), "1.0.0", true));
        }

        @Test
        void gteReturnsFalseWhenActualIsLess() {
            assertFalse(RulesEngine.evaluateExpression(new GteExpression("2.0.0"), "1.9.9", true));
        }
    }

    @Nested
    class LtExpressionTests {
        @Test
        void ltReturnsTrueWhenActualIsLess() {
            assertTrue(RulesEngine.evaluateExpression(new LtExpression("2.0.0"), "1.9.9", true));
        }

        @Test
        void ltReturnsFalseWhenEqual() {
            assertFalse(RulesEngine.evaluateExpression(new LtExpression("1.0.0"), "1.0.0", true));
        }

        @Test
        void ltReturnsFalseWhenActualIsGreater() {
            assertFalse(RulesEngine.evaluateExpression(new LtExpression("1.0.0"), "2.0.0", true));
        }
    }

    @Nested
    class LteExpressionTests {
        @Test
        void lteReturnsTrueWhenActualIsLess() {
            assertTrue(RulesEngine.evaluateExpression(new LteExpression("2.0.0"), "1.9.9", true));
        }

        @Test
        void lteReturnsTrueWhenEqual() {
            assertTrue(RulesEngine.evaluateExpression(new LteExpression("1.0.0"), "1.0.0", true));
        }

        @Test
        void lteReturnsFalseWhenActualIsGreater() {
            assertFalse(RulesEngine.evaluateExpression(new LteExpression("1.0.0"), "2.0.0", true));
        }
    }

    // ---- Requirement 6.6: Semver comparison edge cases ----

    @Nested
    class SemverComparisonTests {
        @Test
        void comparesMinorVersionCorrectly() {
            // 1.0.0 vs 1.0.1 — actual < expected
            assertTrue(RulesEngine.evaluateExpression(new GtExpression("1.0.0"), "1.0.1", true));
            assertFalse(RulesEngine.evaluateExpression(new GtExpression("1.0.1"), "1.0.0", true));
        }

        @Test
        void comparesNumericNotLexicographic() {
            // 2.0.0 vs 10.0.0 — numeric comparison should treat 10 > 2
            assertTrue(RulesEngine.evaluateExpression(new GtExpression("2.0.0"), "10.0.0", true));
            assertFalse(RulesEngine.evaluateExpression(new GtExpression("10.0.0"), "2.0.0", true));
        }

        @Test
        void handlesLargeVersionNumbers() {
            assertTrue(RulesEngine.evaluateExpression(new GteExpression("100.0.0"), "100.0.0", true));
            assertTrue(RulesEngine.evaluateExpression(new GtExpression("99.99.99"), "100.0.0", true));
        }

        @Test
        void handlesTwoPartVersions() {
            // "1.0" is treated as "1.0.0"
            assertEquals(0, RulesEngine.compareSemver("1.0", "1.0.0"));
            assertTrue(RulesEngine.compareSemver("1.1", "1.0.0") > 0);
        }

        @Test
        void handlesSinglePartVersions() {
            // "2" is treated as "2.0.0"
            assertEquals(0, RulesEngine.compareSemver("2", "2.0.0"));
            assertTrue(RulesEngine.compareSemver("2", "1.9.9") > 0);
        }

        @Test
        void handlesQualifierSuffix() {
            // "1.0.0-SNAPSHOT" should strip qualifier and compare as "1.0.0"
            assertEquals(0, RulesEngine.compareSemver("1.0.0-SNAPSHOT", "1.0.0"));
            assertTrue(RulesEngine.compareSemver("1.0.1-beta", "1.0.0") > 0);
        }

        @Test
        void fallsBackToLexicographicForNonSemver() {
            // Non-numeric strings fall back to lexicographic comparison
            int result = RulesEngine.compareSemver("abc", "def");
            assertTrue(result < 0); // "abc" < "def" lexicographically
        }

        @Test
        void handlesEmptyStringGracefully() {
            // Empty string cannot be parsed as semver — falls back to lexicographic
            int result = RulesEngine.compareSemver("", "1.0.0");
            assertTrue(result < 0); // "" < "1.0.0" lexicographically
        }

        @Test
        void lexicographicComparisonWhenSemverDisabled() {
            // When useSemver is false, comparison is lexicographic
            // "9.0.0" > "10.0.0" lexicographically because '9' > '1'
            assertTrue(RulesEngine.evaluateExpression(new GtExpression("10.0.0"), "9.0.0", false));
        }
    }

    // ---- Requirement 6.7: Set expressions ----

    @Nested
    class AnyOfExpressionTests {
        @Test
        void anyOfReturnsTrueWhenValueInSet() {
            AnyOfExpression expr = new AnyOfExpression(List.of("Windows", "Linux", "Mac OS X"));
            assertTrue(RulesEngine.evaluateExpression(expr, "Linux", false));
        }

        @Test
        void anyOfReturnsFalseWhenValueNotInSet() {
            AnyOfExpression expr = new AnyOfExpression(List.of("Windows", "Linux", "Mac OS X"));
            assertFalse(RulesEngine.evaluateExpression(expr, "FreeBSD", false));
        }

        @Test
        void anyOfWithSingleElement() {
            AnyOfExpression expr = new AnyOfExpression(List.of("Eclipse"));
            assertTrue(RulesEngine.evaluateExpression(expr, "Eclipse", false));
            assertFalse(RulesEngine.evaluateExpression(expr, "IntelliJ", false));
        }
    }

    @Nested
    class NoneOfExpressionTests {
        @Test
        void noneOfReturnsTrueWhenValueNotInSet() {
            NoneOfExpression expr = new NoneOfExpression(List.of("Remote", "Cloud"));
            assertTrue(RulesEngine.evaluateExpression(expr, "Local", false));
        }

        @Test
        void noneOfReturnsFalseWhenValueInSet() {
            NoneOfExpression expr = new NoneOfExpression(List.of("Remote", "Cloud"));
            assertFalse(RulesEngine.evaluateExpression(expr, "Remote", false));
        }

        @Test
        void noneOfWithEmptySet() {
            NoneOfExpression expr = new NoneOfExpression(List.of());
            assertTrue(RulesEngine.evaluateExpression(expr, "anything", false));
        }
    }

    // ---- Logical operators ----

    @Nested
    class NotExpressionTests {
        @Test
        void notNegatesTrueToFalse() {
            NotExpression expr = new NotExpression(new EqExpression("Remote"));
            // actual is "Remote", inner == returns true, not negates to false
            assertFalse(RulesEngine.evaluateExpression(expr, "Remote", false));
        }

        @Test
        void notNegatesFalseToTrue() {
            NotExpression expr = new NotExpression(new EqExpression("Remote"));
            // actual is "Local", inner == returns false, not negates to true
            assertTrue(RulesEngine.evaluateExpression(expr, "Local", false));
        }
    }

    @Nested
    class OrExpressionTests {
        @Test
        void orReturnsTrueWhenAnyInnerMatches() {
            OrExpression expr = new OrExpression(List.of(
                    new EqExpression("Windows"),
                    new EqExpression("Linux")
            ));
            assertTrue(RulesEngine.evaluateExpression(expr, "Linux", false));
        }

        @Test
        void orReturnsFalseWhenNoneMatch() {
            OrExpression expr = new OrExpression(List.of(
                    new EqExpression("Windows"),
                    new EqExpression("Linux")
            ));
            assertFalse(RulesEngine.evaluateExpression(expr, "Mac OS X", false));
        }

        @Test
        void orWithSingleExpression() {
            OrExpression expr = new OrExpression(List.of(new EqExpression("Eclipse")));
            assertTrue(RulesEngine.evaluateExpression(expr, "Eclipse", false));
            assertFalse(RulesEngine.evaluateExpression(expr, "IntelliJ", false));
        }
    }

    @Nested
    class AndExpressionTests {
        @Test
        void andReturnsTrueWhenAllInnerMatch() {
            AndExpression expr = new AndExpression(List.of(
                    new GteExpression("1.0.0"),
                    new LtExpression("2.0.0")
            ));
            // actual "1.5.0" is >= 1.0.0 AND < 2.0.0
            assertTrue(RulesEngine.evaluateExpression(expr, "1.5.0", true));
        }

        @Test
        void andReturnsFalseWhenOneInnerFails() {
            AndExpression expr = new AndExpression(List.of(
                    new GteExpression("1.0.0"),
                    new LtExpression("2.0.0")
            ));
            // actual "2.0.0" is >= 1.0.0 but NOT < 2.0.0
            assertFalse(RulesEngine.evaluateExpression(expr, "2.0.0", true));
        }

        @Test
        void andReturnsFalseWhenAllInnerFail() {
            AndExpression expr = new AndExpression(List.of(
                    new GteExpression("3.0.0"),
                    new LtExpression("2.0.0")
            ));
            // actual "2.5.0" is NOT >= 3.0.0 and NOT < 2.0.0
            assertFalse(RulesEngine.evaluateExpression(expr, "2.5.0", true));
        }
    }

    // ---- Nested logical composition ----

    @Nested
    class NestedLogicalExpressionTests {
        @Test
        void notInsideOr() {
            // or(not(== "Remote"), == "Local") — should be true for "Local"
            OrExpression expr = new OrExpression(List.of(
                    new NotExpression(new EqExpression("Remote")),
                    new EqExpression("Local")
            ));
            assertTrue(RulesEngine.evaluateExpression(expr, "Local", false));
        }

        @Test
        void andInsideOr() {
            // or(and(>= "1.0.0", < "2.0.0"), == "3.0.0")
            OrExpression expr = new OrExpression(List.of(
                    new AndExpression(List.of(
                            new GteExpression("1.0.0"),
                            new LtExpression("2.0.0")
                    )),
                    new EqExpression("3.0.0")
            ));
            assertTrue(RulesEngine.evaluateExpression(expr, "1.5.0", true));
            assertTrue(RulesEngine.evaluateExpression(expr, "3.0.0", true));
            assertFalse(RulesEngine.evaluateExpression(expr, "2.5.0", true));
        }
    }

    // ---- Requirement 6.5: Multiple conditions → AND logic ----

    @Nested
    class MatchesAllRulesTests {
        @Test
        void allNullSubConditionsReturnTrue() {
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    null, null, null, null, null
            );
            assertTrue(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void computeOnlyConditionMatchingReturnsTrue() {
            // compute.type == "Local" — Eclipse always reports "Local"
            ComputeType compute = new ComputeType(new EqExpression("Local"), null);
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    compute, null, null, null, null
            );
            assertTrue(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void computeOnlyConditionNotMatchingReturnsFalse() {
            // compute.type == "Remote" — Eclipse always reports "Local"
            ComputeType compute = new ComputeType(new EqExpression("Remote"), null);
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    compute, null, null, null, null
            );
            assertFalse(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void osConditionUsesSystemProperty() {
            // OS type should match the current system's os.name
            String currentOs = System.getProperty("os.name");
            SystemType os = new SystemType(new EqExpression(currentOs), null);
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    null, os, null, null, null
            );
            assertTrue(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void osConditionMismatchReturnsFalse() {
            SystemType os = new SystemType(new EqExpression("NonExistentOS"), null);
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    null, os, null, null, null
            );
            assertFalse(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void multipleConditionsAllPassReturnsTrue() {
            // compute matches (Local) AND os matches (current OS)
            ComputeType compute = new ComputeType(new EqExpression("Local"), null);
            String currentOs = System.getProperty("os.name");
            SystemType os = new SystemType(new EqExpression(currentOs), null);
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    compute, os, null, null, null
            );
            assertTrue(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void multipleConditionsOneFailsReturnsFalse() {
            // compute matches (Local) but os does NOT match
            ComputeType compute = new ComputeType(new EqExpression("Local"), null);
            SystemType os = new SystemType(new EqExpression("NonExistentOS"), null);
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    compute, os, null, null, null
            );
            assertFalse(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void emptyExtensionListReturnsTrueForExtensionCondition() {
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    null, null, null, List.of(), null
            );
            assertTrue(RulesEngine.matchesAllRules(condition));
        }

        @Test
        void nullExtensionListReturnsTrueForExtensionCondition() {
            NotificationDisplayCondition condition = new NotificationDisplayCondition(
                    null, null, null, null, null
            );
            assertTrue(RulesEngine.matchesAllRules(condition));
        }
    }

    // ---- shouldDisplay integration with conditions ----

    @Nested
    class ShouldDisplayTests {
        @Test
        void shouldDisplayWithNoConditionReturnsTrue() {
            NotificationData notification = new NotificationData();
            notification.setCondition(null);
            assertTrue(RulesEngine.shouldDisplay(notification));
        }

        @Test
        void shouldDisplayWithAllNullSubConditionsReturnsTrue() {
            NotificationData notification = new NotificationData();
            notification.setCondition(new NotificationDisplayCondition(null, null, null, null, null));
            assertTrue(RulesEngine.shouldDisplay(notification));
        }

        @Test
        void shouldDisplayWithMatchingComputeReturnsTrue() {
            NotificationData notification = new NotificationData();
            ComputeType compute = new ComputeType(new EqExpression("Local"), null);
            notification.setCondition(new NotificationDisplayCondition(compute, null, null, null, null));
            assertTrue(RulesEngine.shouldDisplay(notification));
        }

        @Test
        void shouldDisplayWithNonMatchingComputeReturnsFalse() {
            NotificationData notification = new NotificationData();
            ComputeType compute = new ComputeType(new EqExpression("Remote"), null);
            notification.setCondition(new NotificationDisplayCondition(compute, null, null, null, null));
            assertFalse(RulesEngine.shouldDisplay(notification));
        }
    }

    // ---- Unknown expression type ----

    @Test
    void unknownExpressionTypeDefaultsToTrue() {
        // Create an anonymous subclass to simulate an unknown expression type
        NotificationExpression unknownExpr = new NotificationExpression() { };
        assertTrue(RulesEngine.evaluateExpression(unknownExpr, "anything", false));
    }
}
