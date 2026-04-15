// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polymorphic base type for notification condition expressions.
 *
 * <p>Uses Jackson wrapper-object polymorphism: the JSON key name (e.g. "==", ">=", "anyOf")
 * determines which concrete subclass to deserialize into.</p>
 *
 * <p>Mirrors the JetBrains v2.0 schema expression types.</p>
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.WRAPPER_OBJECT
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EqExpression.class, name = "=="),
    @JsonSubTypes.Type(value = NeqExpression.class, name = "!="),
    @JsonSubTypes.Type(value = GtExpression.class, name = ">"),
    @JsonSubTypes.Type(value = GteExpression.class, name = ">="),
    @JsonSubTypes.Type(value = LtExpression.class, name = "<"),
    @JsonSubTypes.Type(value = LteExpression.class, name = "<="),
    @JsonSubTypes.Type(value = AnyOfExpression.class, name = "anyOf"),
    @JsonSubTypes.Type(value = NoneOfExpression.class, name = "noneOf"),
    @JsonSubTypes.Type(value = NotExpression.class, name = "not"),
    @JsonSubTypes.Type(value = OrExpression.class, name = "or"),
    @JsonSubTypes.Type(value = AndExpression.class, name = "and")
})
public abstract class NotificationExpression {
}
