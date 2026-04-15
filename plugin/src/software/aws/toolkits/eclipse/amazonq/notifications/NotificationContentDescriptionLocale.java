// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationContentDescriptionLocale {

    @JsonProperty("en-US")
    private NotificationContentDescription locale;

    public NotificationContentDescriptionLocale() {
    }

    public NotificationContentDescriptionLocale(final NotificationContentDescription locale) {
        this.locale = locale;
    }

    public final NotificationContentDescription getLocale() {
        return locale;
    }

    public final void setLocale(final NotificationContentDescription locale) {
        this.locale = locale;
    }
}
