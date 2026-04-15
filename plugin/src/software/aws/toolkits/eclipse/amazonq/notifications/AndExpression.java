// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Logical AND expression ({@code "and"}).
 * Evaluates to true if all of the inner expressions evaluate to true.
 * Deserializes from a JSON array of expression objects,
 * e.g. {@code {"and": [{">=": "1.0.0"}, {"<": "2.0.0"}]}}.
 */
@JsonDeserialize(using = AndExpressionDeserializer.class)
public class AndExpression extends NotificationExpression {

    private final List<NotificationExpression> expressions;

    public AndExpression(final List<NotificationExpression> expressions) {
        this.expressions = expressions;
    }

    public final List<NotificationExpression> getExpressions() {
        return expressions;
    }
}
