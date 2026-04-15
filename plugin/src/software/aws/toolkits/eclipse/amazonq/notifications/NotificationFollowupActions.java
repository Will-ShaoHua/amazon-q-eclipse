// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationFollowupActions {

    @JsonProperty("type")
    private String type;

    @JsonProperty("content")
    private NotificationFollowupActionsContent content;

    public NotificationFollowupActions() {
    }

    public NotificationFollowupActions(final String type, final NotificationFollowupActionsContent content) {
        this.type = type;
        this.content = content;
    }

    public final String getType() {
        return type;
    }

    public final void setType(final String type) {
        this.type = type;
    }

    public final NotificationFollowupActionsContent getContent() {
        return content;
    }

    public final void setContent(final NotificationFollowupActionsContent content) {
        this.content = content;
    }
}
