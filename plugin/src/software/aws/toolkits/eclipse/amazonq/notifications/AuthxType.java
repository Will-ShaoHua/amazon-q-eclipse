// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthxType {

    @JsonProperty("feature")
    private String feature;

    @JsonProperty("type")
    private NotificationExpression type;

    @JsonProperty("region")
    private NotificationExpression region;

    @JsonProperty("connectionState")
    private NotificationExpression connectionState;

    @JsonProperty("ssoScopes")
    private NotificationExpression ssoScopes;

    public AuthxType() {
    }

    public AuthxType(final String feature, final NotificationExpression type, final NotificationExpression region,
                     final NotificationExpression connectionState, final NotificationExpression ssoScopes) {
        this.feature = feature;
        this.type = type;
        this.region = region;
        this.connectionState = connectionState;
        this.ssoScopes = ssoScopes;
    }

    public final String getFeature() {
        return feature;
    }

    public final void setFeature(final String feature) {
        this.feature = feature;
    }

    public final NotificationExpression getType() {
        return type;
    }

    public final void setType(final NotificationExpression type) {
        this.type = type;
    }

    public final NotificationExpression getRegion() {
        return region;
    }

    public final void setRegion(final NotificationExpression region) {
        this.region = region;
    }

    public final NotificationExpression getConnectionState() {
        return connectionState;
    }

    public final void setConnectionState(final NotificationExpression connectionState) {
        this.connectionState = connectionState;
    }

    public final NotificationExpression getSsoScopes() {
        return ssoScopes;
    }

    public final void setSsoScopes(final NotificationExpression ssoScopes) {
        this.ssoScopes = ssoScopes;
    }
}
