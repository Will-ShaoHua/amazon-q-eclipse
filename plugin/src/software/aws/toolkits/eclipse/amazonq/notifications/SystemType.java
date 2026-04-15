// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemType {

    @JsonProperty("type")
    private NotificationExpression type;

    @JsonProperty("version")
    private NotificationExpression version;

    public SystemType() {
    }

    public SystemType(final NotificationExpression type, final NotificationExpression version) {
        this.type = type;
        this.version = version;
    }

    public final NotificationExpression getType() {
        return type;
    }

    public final void setType(final NotificationExpression type) {
        this.type = type;
    }

    public final NotificationExpression getVersion() {
        return version;
    }

    public final void setVersion(final NotificationExpression version) {
        this.version = version;
    }
}
