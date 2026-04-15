// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComputeType {

    @JsonProperty("type")
    private NotificationExpression type;

    @JsonProperty("architecture")
    private NotificationExpression architecture;

    public ComputeType() {
    }

    public ComputeType(final NotificationExpression type, final NotificationExpression architecture) {
        this.type = type;
        this.architecture = architecture;
    }

    public final NotificationExpression getType() {
        return type;
    }

    public final void setType(final NotificationExpression type) {
        this.type = type;
    }

    public final NotificationExpression getArchitecture() {
        return architecture;
    }

    public final void setArchitecture(final NotificationExpression architecture) {
        this.architecture = architecture;
    }
}
