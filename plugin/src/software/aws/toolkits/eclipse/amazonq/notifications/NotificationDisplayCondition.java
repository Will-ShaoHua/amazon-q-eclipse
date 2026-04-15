// Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.eclipse.amazonq.notifications;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationDisplayCondition {

    @JsonProperty("compute")
    private ComputeType compute;

    @JsonProperty("os")
    private SystemType os;

    @JsonProperty("ide")
    private SystemType ide;

    @JsonProperty("extension")
    private List<ExtensionType> extension;

    @JsonProperty("authx")
    private List<AuthxType> authx;

    public NotificationDisplayCondition() {
    }

    public NotificationDisplayCondition(final ComputeType compute, final SystemType os, final SystemType ide,
                                        final List<ExtensionType> extension, final List<AuthxType> authx) {
        this.compute = compute;
        this.os = os;
        this.ide = ide;
        this.extension = extension;
        this.authx = authx;
    }

    public final ComputeType getCompute() {
        return compute;
    }

    public final void setCompute(final ComputeType compute) {
        this.compute = compute;
    }

    public final SystemType getOs() {
        return os;
    }

    public final void setOs(final SystemType os) {
        this.os = os;
    }

    public final SystemType getIde() {
        return ide;
    }

    public final void setIde(final SystemType ide) {
        this.ide = ide;
    }

    public final List<ExtensionType> getExtension() {
        return extension;
    }

    public final void setExtension(final List<ExtensionType> extension) {
        this.extension = extension;
    }

    public final List<AuthxType> getAuthx() {
        return authx;
    }

    public final void setAuthx(final List<AuthxType> authx) {
        this.authx = authx;
    }
}
