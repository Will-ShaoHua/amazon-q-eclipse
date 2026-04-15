// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;

/**
 * Property-based tests for {@link RulesEngine} expression evaluation correctness.
 *
 * <p>Validates: Requirements 6.2, 6.3, 6.4, 6.6, 6.7</p>
 */
@Tag("Feature:maintenance-mode-notification")
public final class RulesEnginePropertyTest {

    // ---- Property 2a: Semver comparison operators (Gt, Gte, Lt, Lte) ----

    /**
     * For any two random semver strings and the GT operator with useSemver=true,
     * evaluateExpression returns true iff the actual version is mathematically
     * greater than the expression version.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void gtExpressionMatchesMathematicalComparison(
            @ForAll("semverStrings") final String actual,
            @ForAll("semverStrings") final String exprValue) {
        int expected = compareSemverLocally(actual, exprValue);
        boolean result = RulesEngine.evaluateExpression(new GtExpression(exprValue), actual, true);
        assertEquals(expected > 0, result,
                String.format("GT: actual=%s, expr=%s, cmp=%d", actual, exprValue, expected));
    }

    /**
     * For any two random semver strings and the GTE operator with useSemver=true,
     * evaluateExpression returns true iff the actual version is mathematically
     * greater than or equal to the expression version.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void gteExpressionMatchesMathematicalComparison(
            @ForAll("semverStrings") final String actual,
            @ForAll("semverStrings") final String exprValue) {
        int expected = compareSemverLocally(actual, exprValue);
        boolean result = RulesEngine.evaluateExpression(new GteExpression(exprValue), actual, true);
        assertEquals(expected >= 0, result,
                String.format("GTE: actual=%s, expr=%s, cmp=%d", actual, exprValue, expected));
    }

    /**
     * For any two random semver strings and the LT operator with useSemver=true,
     * evaluateExpression returns true iff the actual version is mathematically
     * less than the expression version.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void ltExpressionMatchesMathematicalComparison(
            @ForAll("semverStrings") final String actual,
            @ForAll("semverStrings") final String exprValue) {
        int expected = compareSemverLocally(actual, exprValue);
        boolean result = RulesEngine.evaluateExpression(new LtExpression(exprValue), actual, true);
        assertEquals(expected < 0, result,
                String.format("LT: actual=%s, expr=%s, cmp=%d", actual, exprValue, expected));
    }

    /**
     * For any two random semver strings and the LTE operator with useSemver=true,
     * evaluateExpression returns true iff the actual version is mathematically
     * less than or equal to the expression version.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void lteExpressionMatchesMathematicalComparison(
            @ForAll("semverStrings") final String actual,
            @ForAll("semverStrings") final String exprValue) {
        int expected = compareSemverLocally(actual, exprValue);
        boolean result = RulesEngine.evaluateExpression(new LteExpression(exprValue), actual, true);
        assertEquals(expected <= 0, result,
                String.format("LTE: actual=%s, expr=%s, cmp=%d", actual, exprValue, expected));
    }

    // ---- Property 2b: Eq/Neq with string values ----

    /**
     * For any two random strings, EqExpression returns true iff the strings are equal.
     *
     * <p><b>Validates: Requirements 6.3, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void eqExpressionReturnsTrueIffStringsAreEqual(
            @ForAll("safeStrings") final String actual,
            @ForAll("safeStrings") final String exprValue) {
        boolean result = RulesEngine.evaluateExpression(new EqExpression(exprValue), actual, false);
        assertEquals(actual.equals(exprValue), result,
                String.format("EQ: actual=%s, expr=%s", actual, exprValue));
    }

    /**
     * For any two random strings, NeqExpression returns true iff the strings are not equal.
     *
     * <p><b>Validates: Requirements 6.3, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void neqExpressionReturnsTrueIffStringsAreNotEqual(
            @ForAll("safeStrings") final String actual,
            @ForAll("safeStrings") final String exprValue) {
        boolean result = RulesEngine.evaluateExpression(new NeqExpression(exprValue), actual, false);
        assertEquals(!actual.equals(exprValue), result,
                String.format("NEQ: actual=%s, expr=%s", actual, exprValue));
    }

    // ---- Property 2c: AnyOf set membership ----

    /**
     * For any random set of strings and a random actual value,
     * AnyOfExpression returns true iff the actual value is in the set.
     *
     * <p><b>Validates: Requirements 6.4, 6.7</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void anyOfExpressionReturnsTrueIffValueInSet(
            @ForAll("safeStrings") final String actual,
            @ForAll("stringLists") final List<String> values) {
        boolean result = RulesEngine.evaluateExpression(new AnyOfExpression(values), actual, false);
        assertEquals(values.contains(actual), result,
                String.format("ANYOF: actual=%s, values=%s", actual, values));
    }

    // ---- Property 2d: NoneOf set exclusion ----

    /**
     * For any random set of strings and a random actual value,
     * NoneOfExpression returns true iff the actual value is NOT in the set.
     *
     * <p><b>Validates: Requirements 6.4, 6.7</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void noneOfExpressionReturnsTrueIffValueNotInSet(
            @ForAll("safeStrings") final String actual,
            @ForAll("stringLists") final List<String> values) {
        boolean result = RulesEngine.evaluateExpression(new NoneOfExpression(values), actual, false);
        assertEquals(!values.contains(actual), result,
                String.format("NONEOF: actual=%s, values=%s", actual, values));
    }

    // ---- Property 2e: Not expression negation ----

    /**
     * For any random leaf expression and actual value,
     * NotExpression returns the negation of the inner expression result.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void notExpressionNegatesInnerResult(
            @ForAll("leafExpressionWithValue") final ExpressionValuePair pair) {
        boolean innerResult = RulesEngine.evaluateExpression(pair.getExpression(), pair.getActualValue(), pair.isUseSemver());
        boolean notResult = RulesEngine.evaluateExpression(
                new NotExpression(pair.getExpression()), pair.getActualValue(), pair.isUseSemver());
        assertEquals(!innerResult, notResult,
                String.format("NOT: inner=%b, actual=%s", innerResult, pair.getActualValue()));
    }

    // ---- Property 2f: Or expression disjunction ----

    /**
     * For any list of leaf expressions and an actual value,
     * OrExpression returns true iff any inner expression is true.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void orExpressionReturnsTrueIffAnyInnerIsTrue(
            @ForAll("leafExpressionListWithValue") final ExpressionListValuePair pair) {
        boolean orResult = RulesEngine.evaluateExpression(
                new OrExpression(pair.getExpressions()), pair.getActualValue(), pair.isUseSemver());
        boolean expectedAny = false;
        for (NotificationExpression expr : pair.getExpressions()) {
            if (RulesEngine.evaluateExpression(expr, pair.getActualValue(), pair.isUseSemver())) {
                expectedAny = true;
                break;
            }
        }
        assertEquals(expectedAny, orResult,
                String.format("OR: actual=%s, expressionCount=%d", pair.getActualValue(), pair.getExpressions().size()));
    }

    // ---- Property 2g: And expression conjunction ----

    /**
     * For any list of leaf expressions and an actual value,
     * AndExpression returns true iff all inner expressions are true.
     *
     * <p><b>Validates: Requirements 6.2, 6.6</b></p>
     */
    @Property(tries = 100)
    @Tag("Property2:Expression-evaluation-correctness")
    void andExpressionReturnsTrueIffAllInnerAreTrue(
            @ForAll("leafExpressionListWithValue") final ExpressionListValuePair pair) {
        boolean andResult = RulesEngine.evaluateExpression(
                new AndExpression(pair.getExpressions()), pair.getActualValue(), pair.isUseSemver());
        boolean expectedAll = true;
        for (NotificationExpression expr : pair.getExpressions()) {
            if (!RulesEngine.evaluateExpression(expr, pair.getActualValue(), pair.isUseSemver())) {
                expectedAll = false;
                break;
            }
        }
        assertEquals(expectedAll, andResult,
                String.format("AND: actual=%s, expressionCount=%d", pair.getActualValue(), pair.getExpressions().size()));
    }

    // ---- Property 3: Multiple conditions use AND logic ----

    /**
     * For any random combination of condition fields (compute, os, ide), each independently
     * set to null (absent), passing (matches actual runtime value), or failing (does not match),
     * {@code shouldDisplay} returns true if and only if no present condition is in the failing state.
     *
     * <p>This validates that multiple condition fields are combined with AND logic:
     * all present conditions must pass for the notification to be displayable.</p>
     *
     * <p><b>Validates: Requirements 6.5</b></p>
     */
    @Property(tries = 100)
    @Tag("Property3:Multiple-conditions-AND-logic")
    void shouldDisplayReturnsTrueIffAllConditionsPass(
            @ForAll("conditionFieldConfigs") final ConditionFieldConfig config) {
        // Build the condition from the random config
        ComputeType compute = buildComputeCondition(config.getComputeState());
        SystemType os = buildOsCondition(config.getOsState());
        SystemType ide = buildIdeCondition(config.getIdeState());

        NotificationDisplayCondition condition = new NotificationDisplayCondition(
                compute, os, ide, null, null
        );

        NotificationData notification = new NotificationData();
        notification.setCondition(condition);

        boolean result = RulesEngine.shouldDisplay(notification);

        // Expected: true iff no present condition is FAILING
        boolean expectedResult = config.getComputeState() != ConditionState.FAILING
                && config.getOsState() != ConditionState.FAILING
                && config.getIdeState() != ConditionState.FAILING;

        assertEquals(expectedResult, result,
                String.format("AND logic: compute=%s, os=%s, ide=%s",
                        config.getComputeState(), config.getOsState(), config.getIdeState()));
    }

    // ---- Condition builders for Property 3 ----

    private ComputeType buildComputeCondition(final ConditionState state) {
        if (state == ConditionState.ABSENT) {
            return null;
        }
        if (state == ConditionState.PASSING) {
            // Eclipse always reports compute type as "Local"
            return new ComputeType(new EqExpression("Local"), null);
        }
        // FAILING: use a value that will never match
        return new ComputeType(new EqExpression("Remote"), null);
    }

    private SystemType buildOsCondition(final ConditionState state) {
        if (state == ConditionState.ABSENT) {
            return null;
        }
        if (state == ConditionState.PASSING) {
            // Match the actual OS name from the runtime
            return new SystemType(new EqExpression(System.getProperty("os.name")), null);
        }
        // FAILING: use a value that will never match any real OS
        return new SystemType(new EqExpression("NonExistentOS_12345"), null);
    }

    private SystemType buildIdeCondition(final ConditionState state) {
        if (state == ConditionState.ABSENT) {
            return null;
        }
        if (state == ConditionState.PASSING) {
            // Eclipse always reports IDE type as "Eclipse"
            return new SystemType(new EqExpression("Eclipse"), null);
        }
        // FAILING: use a value that will never match
        return new SystemType(new EqExpression("NonExistentIDE_12345"), null);
    }

    // ---- Generators ----

    @Provide
    Arbitrary<ConditionFieldConfig> conditionFieldConfigs() {
        Arbitrary<ConditionState> stateArb = Arbitraries.of(ConditionState.values());
        return Combinators.combine(stateArb, stateArb, stateArb)
                .as(ConditionFieldConfig::new);
    }

    @Provide
    Arbitrary<String> semverStrings() {
        return Combinators.combine(
                Arbitraries.integers().between(0, 999),
                Arbitraries.integers().between(0, 999),
                Arbitraries.integers().between(0, 999)
        ).as((major, minor, patch) -> major + "." + minor + "." + patch);
    }

    @Provide
    Arbitrary<String> safeStrings() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('.', '-', '_')
                .ofMinLength(1)
                .ofMaxLength(20);
    }

    @Provide
    Arbitrary<List<String>> stringLists() {
        return safeStrings().list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<ExpressionValuePair> leafExpressionWithValue() {
        return Arbitraries.oneOf(
                semverLeafExpressionWithValue(),
                stringLeafExpressionWithValue()
        );
    }

    private Arbitrary<ExpressionValuePair> semverLeafExpressionWithValue() {
        return Combinators.combine(semverStrings(), semverLeafExpressions())
                .as((actual, expr) -> new ExpressionValuePair(expr, actual, true));
    }

    private Arbitrary<ExpressionValuePair> stringLeafExpressionWithValue() {
        return Combinators.combine(safeStrings(), stringLeafExpressions())
                .as((actual, expr) -> new ExpressionValuePair(expr, actual, false));
    }

    private Arbitrary<NotificationExpression> semverLeafExpressions() {
        return Arbitraries.oneOf(
                semverStrings().map(GtExpression::new),
                semverStrings().map(GteExpression::new),
                semverStrings().map(LtExpression::new),
                semverStrings().map(LteExpression::new),
                semverStrings().map(EqExpression::new),
                semverStrings().map(NeqExpression::new)
        );
    }

    private Arbitrary<NotificationExpression> stringLeafExpressions() {
        return Arbitraries.oneOf(
                safeStrings().map(EqExpression::new),
                safeStrings().map(NeqExpression::new),
                safeStrings().list().ofMinSize(1).ofMaxSize(5).map(AnyOfExpression::new),
                safeStrings().list().ofMinSize(1).ofMaxSize(5).map(NoneOfExpression::new)
        );
    }

    @Provide
    Arbitrary<ExpressionListValuePair> leafExpressionListWithValue() {
        return Arbitraries.oneOf(
                semverExpressionListWithValue(),
                stringExpressionListWithValue()
        );
    }

    private Arbitrary<ExpressionListValuePair> semverExpressionListWithValue() {
        return Combinators.combine(
                semverStrings(),
                semverLeafExpressions().list().ofMinSize(1).ofMaxSize(5)
        ).as((actual, exprs) -> new ExpressionListValuePair(exprs, actual, true));
    }

    private Arbitrary<ExpressionListValuePair> stringExpressionListWithValue() {
        return Combinators.combine(
                safeStrings(),
                stringLeafExpressions().list().ofMinSize(1).ofMaxSize(5)
        ).as((actual, exprs) -> new ExpressionListValuePair(exprs, actual, false));
    }

    // ---- Helper types ----

    /**
     * Represents the three possible states for a condition field in Property 3 testing.
     */
    enum ConditionState {
        /** Condition field is null (not present) — always passes. */
        ABSENT,
        /** Condition field is present and matches the actual runtime value — passes. */
        PASSING,
        /** Condition field is present but does NOT match the actual runtime value — fails. */
        FAILING
    }

    /**
     * Configuration for a random combination of condition field states.
     * Each of the three testable condition fields (compute, os, ide) is independently
     * assigned one of the three states: ABSENT, PASSING, or FAILING.
     */
    static final class ConditionFieldConfig {
        private final ConditionState computeState;
        private final ConditionState osState;
        private final ConditionState ideState;

        ConditionFieldConfig(final ConditionState computeState,
                             final ConditionState osState,
                             final ConditionState ideState) {
            this.computeState = computeState;
            this.osState = osState;
            this.ideState = ideState;
        }

        ConditionState getComputeState() {
            return computeState;
        }

        ConditionState getOsState() {
            return osState;
        }

        ConditionState getIdeState() {
            return ideState;
        }

        @Override
        public String toString() {
            return String.format("ConditionFieldConfig{compute=%s, os=%s, ide=%s}",
                    computeState, osState, ideState);
        }
    }

    /**
     * Pairs a leaf expression with an actual value and useSemver flag for property testing.
     */
    static final class ExpressionValuePair {
        private final NotificationExpression expression;
        private final String actualValue;
        private final boolean useSemver;

        ExpressionValuePair(final NotificationExpression expression,
                            final String actualValue,
                            final boolean useSemver) {
            this.expression = expression;
            this.actualValue = actualValue;
            this.useSemver = useSemver;
        }

        NotificationExpression getExpression() {
            return expression;
        }

        String getActualValue() {
            return actualValue;
        }

        boolean isUseSemver() {
            return useSemver;
        }
    }

    /**
     * Pairs a list of leaf expressions with an actual value and useSemver flag for property testing.
     */
    static final class ExpressionListValuePair {
        private final List<NotificationExpression> expressions;
        private final String actualValue;
        private final boolean useSemver;

        ExpressionListValuePair(final List<NotificationExpression> expressions,
                                final String actualValue,
                                final boolean useSemver) {
            this.expressions = expressions;
            this.actualValue = actualValue;
            this.useSemver = useSemver;
        }

        List<NotificationExpression> getExpressions() {
            return expressions;
        }

        String getActualValue() {
            return actualValue;
        }

        boolean isUseSemver() {
            return useSemver;
        }
    }

    // ---- Local semver comparison (oracle) ----

    /**
     * Independent semver comparison implementation used as the test oracle.
     * Compares two semver strings component by component.
     */
    private static int compareSemverLocally(final String a, final String b) {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int aVal = i < aParts.length ? Integer.parseInt(aParts[i]) : 0;
            int bVal = i < bParts.length ? Integer.parseInt(bParts[i]) : 0;
            if (aVal != bVal) {
                return Integer.compare(aVal, bVal);
            }
        }
        return 0;
    }
}
