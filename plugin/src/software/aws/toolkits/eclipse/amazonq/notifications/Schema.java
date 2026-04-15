// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Schema {

    @JsonProperty("version")
    private String version;

    public Schema() {
    }

    public Schema(final String version) {
        this.version = version;
    }

    public final String getVersion() {
        return version;
    }

    public final void setVersion(final String version) {
        this.version = version;
    }
}
