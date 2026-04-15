// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom deserializer for {@link NotExpression}.
 * Reads the inner JSON object and deserializes it as a {@link NotificationExpression}.
 */
public final class NotExpressionDeserializer extends JsonDeserializer<NotExpression> {

    @Override
    public NotExpression deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        JsonParser innerParser = node.traverse(parser.getCodec());
        innerParser.nextToken();
        NotificationExpression inner = innerParser.readValueAs(NotificationExpression.class);
        return new NotExpression(inner);
    }
}
