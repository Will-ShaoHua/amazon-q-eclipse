// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import software.aws.toolkits.eclipse.amazonq.plugin.Activator;

/**
 * Evaluates notification display conditions against the local Eclipse environment.
 *
 * <p>Mirrors the JetBrains {@code RulesEngine} pattern: each notification may carry
 * a {@link NotificationDisplayCondition} with compute, OS, IDE, extension, and authx
 * sub-conditions. All present sub-conditions are combined with AND logic — a notification
 * is displayable only when every sub-condition passes.</p>
 *
 * <p>Expression evaluation supports comparison operators ({@code ==}, {@code !=},
 * {@code >}, {@code >=}, {@code <}, {@code <=}), set operators ({@code anyOf},
 * {@code noneOf}), and logical operators ({@code not}, {@code or}, {@code and}).
 * Version fields use semantic version comparison; all other fields use
 * lexicographic comparison.</p>
 */
public final class RulesEngine {

    private RulesEngine() {
        // Utility class — prevent instantiation
    }

    /**
     * Determines whether a notification should be displayed to the current user.
     *
     * @param notification the notification to evaluate
     * @return {@code true} if the notification has no condition or all conditions are satisfied
     */
    public static boolean shouldDisplay(final NotificationData notification) {
        if (notification.getCondition() == null) {
            return true;
        }
        return matchesAllRules(notification.getCondition());
    }

    /**
     * Evaluates all sub-conditions in a {@link NotificationDisplayCondition} using AND logic.
     * A {@code null} sub-condition is treated as satisfied (true).
     */
    static boolean matchesAllRules(final NotificationDisplayCondition condition) {
        boolean compute = condition.getCompute() == null || matchesCompute(condition.getCompute());
        boolean os = condition.getOs() == null || matchesOs(condition.getOs());
        boolean ide = condition.getIde() == null || matchesIde(condition.getIde());
        boolean extension = matchesExtension(condition.getExtension());
        // authx is not evaluated in Eclipse (no auth connection context available)
        return compute && os && ide && extension;
    }

    // ---- Compute condition ----

    private static boolean matchesCompute(final ComputeType compute) {
        // Eclipse always runs locally
        String actualType = "Local";
        String actualArchitecture = System.getProperty("os.arch");

        boolean type = compute.getType() == null
                || evaluateExpression(compute.getType(), actualType, false);
        boolean architecture = compute.getArchitecture() == null
                || evaluateExpression(compute.getArchitecture(), actualArchitecture, false);
        return type && architecture;
    }

    // ---- OS condition ----

    private static boolean matchesOs(final SystemType osCondition) {
        String actualOsType = System.getProperty("os.name");
        String actualOsVersion = System.getProperty("os.version");

        boolean type = osCondition.getType() == null
                || evaluateExpression(osCondition.getType(), actualOsType, false);
        boolean version = osCondition.getVersion() == null
                || evaluateExpression(osCondition.getVersion(), actualOsVersion, false);
        return type && version;
    }

    // ---- IDE condition ----

    private static boolean matchesIde(final SystemType ideCondition) {
        String actualIdeType = "Eclipse";
        String actualIdeVersion = System.getProperty("eclipse.buildId") != null
                ? System.getProperty("eclipse.buildId") : "";

        boolean type = ideCondition.getType() == null
                || evaluateExpression(ideCondition.getType(), actualIdeType, false);
        boolean version = ideCondition.getVersion() == null
                || evaluateExpression(ideCondition.getVersion(), actualIdeVersion, true);
        return type && version;
    }

    // ---- Extension condition ----

    private static boolean matchesExtension(final List<ExtensionType> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return true;
        }

        Map<String, String> pluginVersions = getPluginVersionMap();

        // Collect the extension IDs the notification cares about
        boolean anyPluginFound = false;
        for (ExtensionType ext : extensions) {
            if (ext.getId() != null && pluginVersions.containsKey(ext.getId())) {
                anyPluginFound = true;
                break;
            }
        }
        if (!anyPluginFound) {
            return false;
        }

        // SNAPSHOT versions are development builds — skip notifications
        for (String version : pluginVersions.values()) {
            if (version.toUpperCase().contains("SNAPSHOT")) {
                return false;
            }
        }

        for (ExtensionType ext : extensions) {
            String actualVersion = pluginVersions.get(ext.getId());
            if (actualVersion == null) {
                // Extension not installed — treat as satisfied (consistent with JetBrains)
                continue;
            }
            if (ext.getVersion() != null
                    && !evaluateExpression(ext.getVersion(), actualVersion, true)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a map of installed plugin IDs to their version strings.
     * Currently only includes the Amazon Q Eclipse plugin.
     */
    private static Map<String, String> getPluginVersionMap() {
        Map<String, String> versions = new HashMap<>();
        try {
            Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
            if (bundle != null) {
                Version v = bundle.getVersion();
                // Format as major.minor.micro to match semver expectations
                String versionStr = v.getMajor() + "." + v.getMinor() + "." + v.getMicro();
                versions.put(Activator.PLUGIN_ID, versionStr);
            }
        } catch (Exception e) {
            Activator.getLogger().warn("Failed to resolve plugin version for extension condition evaluation", e);
        }
        return versions;
    }

    // ---- Expression evaluation ----

    /**
     * Evaluates a single {@link NotificationExpression} against an actual value.
     *
     * @param expr       the expression to evaluate
     * @param actualValue the runtime value to compare against
     * @param useSemver  if {@code true}, comparison operators use semantic version comparison;
     *                   otherwise lexicographic comparison is used
     * @return {@code true} if the expression is satisfied
     */
    public static boolean evaluateExpression(final NotificationExpression expr,
                                             final String actualValue,
                                             final boolean useSemver) {
        if (expr instanceof EqExpression) {
            return ((EqExpression) expr).getValue().equals(actualValue);

        } else if (expr instanceof NeqExpression) {
            return !((NeqExpression) expr).getValue().equals(actualValue);

        } else if (expr instanceof GtExpression) {
            return compare(actualValue, ((GtExpression) expr).getValue(), useSemver) > 0;

        } else if (expr instanceof GteExpression) {
            return compare(actualValue, ((GteExpression) expr).getValue(), useSemver) >= 0;

        } else if (expr instanceof LtExpression) {
            return compare(actualValue, ((LtExpression) expr).getValue(), useSemver) < 0;

        } else if (expr instanceof LteExpression) {
            return compare(actualValue, ((LteExpression) expr).getValue(), useSemver) <= 0;

        } else if (expr instanceof AnyOfExpression) {
            return ((AnyOfExpression) expr).getValues().contains(actualValue);

        } else if (expr instanceof NoneOfExpression) {
            return !((NoneOfExpression) expr).getValues().contains(actualValue);

        } else if (expr instanceof NotExpression) {
            return !evaluateExpression(((NotExpression) expr).getExpression(), actualValue, useSemver);

        } else if (expr instanceof OrExpression) {
            for (NotificationExpression inner : ((OrExpression) expr).getExpressions()) {
                if (evaluateExpression(inner, actualValue, useSemver)) {
                    return true;
                }
            }
            return false;

        } else if (expr instanceof AndExpression) {
            for (NotificationExpression inner : ((AndExpression) expr).getExpressions()) {
                if (!evaluateExpression(inner, actualValue, useSemver)) {
                    return false;
                }
            }
            return true;
        }

        // Unknown expression type — default to true (show notification)
        return true;
    }

    // ---- Comparison helpers ----

    /**
     * Compares two version or string values.
     * When {@code useSemver} is true, attempts semantic version comparison first;
     * falls back to lexicographic comparison if either string is not a valid semver.
     */
    private static int compare(final String actual, final String expected, final boolean useSemver) {
        if (useSemver) {
            return compareSemver(actual, expected);
        }
        return actual.compareTo(expected);
    }

    /**
     * Compares two strings as semantic versions (major.minor.patch).
     * Falls back to lexicographic comparison if either string cannot be parsed.
     */
    static int compareSemver(final String actual, final String expected) {
        int[] actualParts = parseSemver(actual);
        int[] expectedParts = parseSemver(expected);

        if (actualParts == null || expectedParts == null) {
            return actual.compareTo(expected);
        }

        for (int i = 0; i < 3; i++) {
            if (actualParts[i] != expectedParts[i]) {
                return Integer.compare(actualParts[i], expectedParts[i]);
            }
        }
        return 0;
    }

    /**
     * Parses a version string into a 3-element int array [major, minor, patch].
     * Accepts formats like "1.0.0", "1.0", or "1". Missing components default to 0.
     * Returns {@code null} if the string cannot be parsed.
     */
    private static int[] parseSemver(final String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }

        // Strip any qualifier suffix (e.g., "1.0.0-SNAPSHOT" → "1.0.0")
        String cleaned = version;
        int dashIndex = version.indexOf('-');
        if (dashIndex >= 0) {
            cleaned = version.substring(0, dashIndex);
        }

        String[] parts = cleaned.split("\\.");
        int[] result = new int[3];
        try {
            for (int i = 0; i < Math.min(parts.length, 3); i++) {
                result[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }
}
