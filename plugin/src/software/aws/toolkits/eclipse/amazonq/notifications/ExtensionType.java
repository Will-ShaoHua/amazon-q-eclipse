// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtensionType {

    @JsonProperty("id")
    private String id;

    @JsonProperty("version")
    private NotificationExpression version;

    public ExtensionType() {
    }

    public ExtensionType(final String id, final NotificationExpression version) {
        this.id = id;
        this.version = version;
    }

    public final String getId() {
        return id;
    }

    public final void setId(final String id) {
        this.id = id;
    }

    public final NotificationExpression getVersion() {
        return version;
    }

    public final void setVersion(final NotificationExpression version) {
        this.version = version;
    }
}
