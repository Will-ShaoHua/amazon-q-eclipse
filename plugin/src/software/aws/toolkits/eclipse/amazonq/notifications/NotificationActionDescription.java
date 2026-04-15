// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationActionDescription {

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    public NotificationActionDescription() {
    }

    public NotificationActionDescription(final String title, final String url) {
        this.title = title;
        this.url = url;
    }

    public final String getTitle() {
        return title;
    }

    public final void setTitle(final String title) {
        this.title = title;
    }

    public final String getUrl() {
        return url;
    }

    public final void setUrl(final String url) {
        this.url = url;
    }
}
