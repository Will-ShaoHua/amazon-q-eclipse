// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationsList {

    @JsonProperty("schema")
    private Schema schema;

    @JsonProperty("notifications")
    private List<NotificationData> notifications;

    public NotificationsList() {
    }

    public NotificationsList(final Schema schema, final List<NotificationData> notifications) {
        this.schema = schema;
        this.notifications = notifications;
    }

    public final Schema getSchema() {
        return schema;
    }

    public final void setSchema(final Schema schema) {
        this.schema = schema;
    }

    public final List<NotificationData> getNotifications() {
        return notifications;
    }

    public final void setNotifications(final List<NotificationData> notifications) {
        this.notifications = notifications;
    }
}
