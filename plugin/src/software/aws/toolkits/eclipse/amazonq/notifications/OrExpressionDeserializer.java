// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom deserializer for {@link OrExpression}.
 * Reads a JSON array of expression objects and deserializes each as a {@link NotificationExpression}.
 */
public final class OrExpressionDeserializer extends JsonDeserializer<OrExpression> {

    @Override
    public OrExpression deserialize(final JsonParser parser, final DeserializationContext ctxt) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        List<NotificationExpression> expressions = new ArrayList<>();
        for (JsonNode element : node) {
            JsonParser elementParser = element.traverse(parser.getCodec());
            elementParser.nextToken();
            expressions.add(elementParser.readValueAs(NotificationExpression.class));
        }
        return new OrExpression(expressions);
    }
}
