// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationData {

    @JsonProperty("id")
    private String id;

    @JsonProperty("schedule")
    private NotificationSchedule schedule;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("condition")
    private NotificationDisplayCondition condition;

    @JsonProperty("content")
    private NotificationContentDescriptionLocale content;

    @JsonProperty("actions")
    private List<NotificationFollowupActions> actions;

    public NotificationData() {
    }

    public NotificationData(final String id, final NotificationSchedule schedule, final String severity,
                            final NotificationDisplayCondition condition, final NotificationContentDescriptionLocale content,
                            final List<NotificationFollowupActions> actions) {
        this.id = id;
        this.schedule = schedule;
        this.severity = severity;
        this.condition = condition;
        this.content = content;
        this.actions = actions;
    }

    public final String getId() {
        return id;
    }

    public final void setId(final String id) {
        this.id = id;
    }

    public final NotificationSchedule getSchedule() {
        return schedule;
    }

    public final void setSchedule(final NotificationSchedule schedule) {
        this.schedule = schedule;
    }

    public final String getSeverity() {
        return severity;
    }

    public final void setSeverity(final String severity) {
        this.severity = severity;
    }

    public final NotificationDisplayCondition getCondition() {
        return condition;
    }

    public final void setCondition(final NotificationDisplayCondition condition) {
        this.condition = condition;
    }

    public final NotificationContentDescriptionLocale getContent() {
        return content;
    }

    public final void setContent(final NotificationContentDescriptionLocale content) {
        this.content = content;
    }

    public final List<NotificationFollowupActions> getActions() {
        return actions;
    }

    public final void setActions(final List<NotificationFollowupActions> actions) {
        this.actions = actions;
    }
}
