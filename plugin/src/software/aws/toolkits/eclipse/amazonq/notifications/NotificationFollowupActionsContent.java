// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationFollowupActionsContent {

    @JsonProperty("en-US")
    private NotificationActionDescription locale;

    public NotificationFollowupActionsContent() {
    }

    public NotificationFollowupActionsContent(final NotificationActionDescription locale) {
        this.locale = locale;
    }

    public final NotificationActionDescription getLocale() {
        return locale;
    }

    public final void setLocale(final NotificationActionDescription locale) {
        this.locale = locale;
    }
}
