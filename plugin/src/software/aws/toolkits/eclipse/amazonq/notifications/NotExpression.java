// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Logical NOT expression ({@code "not"}).
 * Negates the result of the inner expression.
 * Deserializes from a nested expression object, e.g. {@code {"not": {"==": "Remote"}}}.
 */
@JsonDeserialize(using = NotExpressionDeserializer.class)
public class NotExpression extends NotificationExpression {

    private final NotificationExpression expression;

    public NotExpression(final NotificationExpression expression) {
        this.expression = expression;
    }

    public final NotificationExpression getExpression() {
        return expression;
    }
}
