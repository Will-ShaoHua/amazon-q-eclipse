// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Set membership expression ({@code "anyOf"}).
 * Evaluates to true if the actual value is contained in the provided list.
 * Deserializes from a JSON array, e.g. {@code {"anyOf": ["Windows", "Linux", "Mac OS X"]}}.
 */
public class AnyOfExpression extends NotificationExpression {

    private final List<String> values;

    @JsonCreator
    public AnyOfExpression(final List<String> values) {
        this.values = values;
    }

    @JsonValue
    public final List<String> getValues() {
        return values;
    }
}
