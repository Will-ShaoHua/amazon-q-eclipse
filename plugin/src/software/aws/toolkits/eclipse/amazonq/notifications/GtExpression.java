// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Greater-than comparison expression ({@code ">"}).
 * Deserializes from a single string value, e.g. {@code {">": "1.0.0"}}.
 */
public class GtExpression extends NotificationExpression {

    private final String value;

    @JsonCreator
    public GtExpression(final String value) {
        this.value = value;
    }

    @JsonValue
    public final String getValue() {
        return value;
    }
}
