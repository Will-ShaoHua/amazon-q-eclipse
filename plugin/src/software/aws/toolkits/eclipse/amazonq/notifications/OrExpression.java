// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Logical OR expression ({@code "or"}).
 * Evaluates to true if any of the inner expressions evaluate to true.
 * Deserializes from a JSON array of expression objects,
 * e.g. {@code {"or": [{"==": "Windows"}, {"==": "Linux"}]}}.
 */
@JsonDeserialize(using = OrExpressionDeserializer.class)
public class OrExpression extends NotificationExpression {

    private final List<NotificationExpression> expressions;

    public OrExpression(final List<NotificationExpression> expressions) {
        this.expressions = expressions;
    }

    public final List<NotificationExpression> getExpressions() {
        return expressions;
    }
}
