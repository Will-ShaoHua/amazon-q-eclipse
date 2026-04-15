// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class NotificationDataModelTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testFullPayloadDeserialization() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "test-notification-001",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "compute": {
                          "type": { "==": "Local" },
                          "architecture": { "anyOf": ["x86_64", "aarch64"] }
                        },
                        "os": {
                          "type": { "anyOf": ["Windows", "Linux", "Mac OS X"] },
                          "version": { ">=": "10.0.0" }
                        },
                        "ide": {
                          "type": { "==": "Eclipse" },
                          "version": { ">=": "4.20.0" }
                        },
                        "extension": [
                          { "id": "amazon.q", "version": { ">=": "1.0.0" } }
                        ],
                        "authx": [
                          {
                            "feature": "codewhisperer",
                            "type": { "==": "SSO" },
                            "region": { "anyOf": ["us-east-1", "us-west-2"] },
                            "connectionState": { "==": "connected" },
                            "ssoScopes": { "anyOf": ["codewhisperer:completions", "codewhisperer:analysis"] }
                          }
                        ]
                      },
                      "content": {
                        "en-US": {
                          "title": "Amazon Q Maintenance",
                          "description": "Amazon Q services are undergoing planned maintenance."
                        }
                      },
                      "actions": [
                        {
                          "type": "openUrl",
                          "content": {
                            "en-US": {
                              "title": "View Status",
                              "url": "https://health.aws.amazon.com/health/status"
                            }
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);

        assertNotNull(result);
        assertNotNull(result.getSchema());
        assertEquals("2.0", result.getSchema().getVersion());
        assertNotNull(result.getNotifications());
        assertEquals(1, result.getNotifications().size());

        NotificationData notification = result.getNotifications().get(0);
        assertEquals("test-notification-001", notification.getId());
        assertEquals("Emergency", notification.getSchedule().getType());
        assertEquals("Critical", notification.getSeverity());

        // Verify condition
        NotificationDisplayCondition condition = notification.getCondition();
        assertNotNull(condition);

        // Compute condition
        ComputeType compute = condition.getCompute();
        assertNotNull(compute);
        assertInstanceOf(EqExpression.class, compute.getType());
        assertEquals("Local", ((EqExpression) compute.getType()).getValue());
        assertInstanceOf(AnyOfExpression.class, compute.getArchitecture());
        assertEquals(List.of("x86_64", "aarch64"), ((AnyOfExpression) compute.getArchitecture()).getValues());

        // OS condition
        SystemType os = condition.getOs();
        assertNotNull(os);
        assertInstanceOf(AnyOfExpression.class, os.getType());
        assertEquals(List.of("Windows", "Linux", "Mac OS X"), ((AnyOfExpression) os.getType()).getValues());
        assertInstanceOf(GteExpression.class, os.getVersion());
        assertEquals("10.0.0", ((GteExpression) os.getVersion()).getValue());

        // IDE condition
        SystemType ide = condition.getIde();
        assertNotNull(ide);
        assertInstanceOf(EqExpression.class, ide.getType());
        assertEquals("Eclipse", ((EqExpression) ide.getType()).getValue());
        assertInstanceOf(GteExpression.class, ide.getVersion());
        assertEquals("4.20.0", ((GteExpression) ide.getVersion()).getValue());

        // Extension condition
        List<ExtensionType> extensions = condition.getExtension();
        assertNotNull(extensions);
        assertEquals(1, extensions.size());
        assertEquals("amazon.q", extensions.get(0).getId());
        assertInstanceOf(GteExpression.class, extensions.get(0).getVersion());
        assertEquals("1.0.0", ((GteExpression) extensions.get(0).getVersion()).getValue());

        // Authx condition
        List<AuthxType> authxList = condition.getAuthx();
        assertNotNull(authxList);
        assertEquals(1, authxList.size());
        AuthxType authx = authxList.get(0);
        assertEquals("codewhisperer", authx.getFeature());
        assertInstanceOf(EqExpression.class, authx.getType());
        assertEquals("SSO", ((EqExpression) authx.getType()).getValue());
        assertInstanceOf(AnyOfExpression.class, authx.getRegion());
        assertEquals(List.of("us-east-1", "us-west-2"), ((AnyOfExpression) authx.getRegion()).getValues());
        assertInstanceOf(EqExpression.class, authx.getConnectionState());
        assertEquals("connected", ((EqExpression) authx.getConnectionState()).getValue());
        assertInstanceOf(AnyOfExpression.class, authx.getSsoScopes());

        // Content
        NotificationContentDescriptionLocale content = notification.getContent();
        assertNotNull(content);
        assertNotNull(content.getLocale());
        assertEquals("Amazon Q Maintenance", content.getLocale().getTitle());
        assertEquals("Amazon Q services are undergoing planned maintenance.", content.getLocale().getDescription());

        // Actions
        List<NotificationFollowupActions> actions = notification.getActions();
        assertNotNull(actions);
        assertEquals(1, actions.size());
        assertEquals("openUrl", actions.get(0).getType());
        assertNotNull(actions.get(0).getContent());
        assertNotNull(actions.get(0).getContent().getLocale());
        assertEquals("View Status", actions.get(0).getContent().getLocale().getTitle());
        assertEquals("https://health.aws.amazon.com/health/status", actions.get(0).getContent().getLocale().getUrl());
    }

    @Test
    void testEmptyNotificationsArray() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": []
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);

        assertNotNull(result);
        assertEquals("2.0", result.getSchema().getVersion());
        assertNotNull(result.getNotifications());
        assertTrue(result.getNotifications().isEmpty());
    }

    @Test
    void testNullCondition() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "no-condition-notification",
                      "schedule": { "type": "Startup" },
                      "severity": "Warning",
                      "content": {
                        "en-US": {
                          "title": "Update Available",
                          "description": "A new version is available."
                        }
                      }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);

        assertNotNull(result);
        assertEquals(1, result.getNotifications().size());
        NotificationData notification = result.getNotifications().get(0);
        assertEquals("no-condition-notification", notification.getId());
        assertEquals("Startup", notification.getSchedule().getType());
        assertEquals("Warning", notification.getSeverity());
        assertNull(notification.getCondition());
        assertNull(notification.getActions());
        assertNotNull(notification.getContent());
        assertEquals("Update Available", notification.getContent().getLocale().getTitle());
    }

    @Test
    void testEqExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "eq-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "compute": { "type": { "==": "Local" } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getCompute().getType();

        assertInstanceOf(EqExpression.class, expr);
        assertEquals("Local", ((EqExpression) expr).getValue());
    }

    @Test
    void testNeqExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "neq-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "compute": { "type": { "!=": "Remote" } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getCompute().getType();

        assertInstanceOf(NeqExpression.class, expr);
        assertEquals("Remote", ((NeqExpression) expr).getValue());
    }

    @Test
    void testGtExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "gt-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "version": { ">": "10.0.0" } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getVersion();

        assertInstanceOf(GtExpression.class, expr);
        assertEquals("10.0.0", ((GtExpression) expr).getValue());
    }

    @Test
    void testGteExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "gte-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "version": { ">=": "11.0.0" } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getVersion();

        assertInstanceOf(GteExpression.class, expr);
        assertEquals("11.0.0", ((GteExpression) expr).getValue());
    }

    @Test
    void testLtExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "lt-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "version": { "<": "15.0.0" } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getVersion();

        assertInstanceOf(LtExpression.class, expr);
        assertEquals("15.0.0", ((LtExpression) expr).getValue());
    }

    @Test
    void testLteExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "lte-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "version": { "<=": "14.0.0" } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getVersion();

        assertInstanceOf(LteExpression.class, expr);
        assertEquals("14.0.0", ((LteExpression) expr).getValue());
    }

    @Test
    void testAnyOfExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "anyof-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "type": { "anyOf": ["Windows", "Linux", "Mac OS X"] } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getType();

        assertInstanceOf(AnyOfExpression.class, expr);
        assertEquals(List.of("Windows", "Linux", "Mac OS X"), ((AnyOfExpression) expr).getValues());
    }

    @Test
    void testNoneOfExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "noneof-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "compute": { "type": { "noneOf": ["Remote", "Cloud"] } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getCompute().getType();

        assertInstanceOf(NoneOfExpression.class, expr);
        assertEquals(List.of("Remote", "Cloud"), ((NoneOfExpression) expr).getValues());
    }

    @Test
    void testNotExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "not-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "compute": { "type": { "not": { "==": "Remote" } } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getCompute().getType();

        assertInstanceOf(NotExpression.class, expr);
        NotExpression notExpr = (NotExpression) expr;
        assertInstanceOf(EqExpression.class, notExpr.getExpression());
        assertEquals("Remote", ((EqExpression) notExpr.getExpression()).getValue());
    }

    @Test
    void testOrExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "or-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "type": { "or": [{ "==": "Windows" }, { "==": "Linux" }] } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getType();

        assertInstanceOf(OrExpression.class, expr);
        OrExpression orExpr = (OrExpression) expr;
        assertEquals(2, orExpr.getExpressions().size());
        assertInstanceOf(EqExpression.class, orExpr.getExpressions().get(0));
        assertEquals("Windows", ((EqExpression) orExpr.getExpressions().get(0)).getValue());
        assertInstanceOf(EqExpression.class, orExpr.getExpressions().get(1));
        assertEquals("Linux", ((EqExpression) orExpr.getExpressions().get(1)).getValue());
    }

    @Test
    void testAndExpression() throws Exception {
        String json = """
                {
                  "schema": { "version": "2.0" },
                  "notifications": [
                    {
                      "id": "and-test",
                      "schedule": { "type": "Emergency" },
                      "severity": "Critical",
                      "condition": {
                        "os": { "version": { "and": [{ ">=": "10.0.0" }, { "<": "15.0.0" }] } }
                      },
                      "content": { "en-US": { "title": "T", "description": "D" } }
                    }
                  ]
                }
                """;

        NotificationsList result = objectMapper.readValue(json, NotificationsList.class);
        NotificationExpression expr = result.getNotifications().get(0).getCondition().getOs().getVersion();

        assertInstanceOf(AndExpression.class, expr);
        AndExpression andExpr = (AndExpression) expr;
        assertEquals(2, andExpr.getExpressions().size());
        assertInstanceOf(GteExpression.class, andExpr.getExpressions().get(0));
        assertEquals("10.0.0", ((GteExpression) andExpr.getExpressions().get(0)).getValue());
        assertInstanceOf(LtExpression.class, andExpr.getExpressions().get(1));
        assertEquals("15.0.0", ((LtExpression) andExpr.getExpressions().get(1)).getValue());
    }
}
