/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.execution;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.tenant.TenantContext;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Ivica Cardic
 */
public class ApprovalId implements Serializable {

    private final boolean approved;
    private final long jobId;
    private final String tenantId;
    private final String uuid;

    private ApprovalId(String tenantId, long jobId, String uuid, boolean approved) {
        this.approved = approved;
        this.jobId = jobId;
        this.tenantId = tenantId;

        this.uuid = uuid;
    }

    public static ApprovalId of(long jobId, boolean approved) {
        UUID uuid = UUID.randomUUID();

        return new ApprovalId(TenantContext.getCurrentTenantId(), jobId, uuid.toString(), approved);
    }

    public static ApprovalId parse(String id) {
        id = EncodingUtils.base64DecodeToString(id);

        String[] items = id.split(":");

        return new ApprovalId(items[0], Long.parseLong(items[1]), items[2], Boolean.parseBoolean(items[3]));
    }

    public long getJobId() {
        return jobId;
    }

    public String getTenantId() {
        return Objects.requireNonNull(tenantId);
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isApproved() {
        return approved;
    }

    @Override
    public String toString() {
        return EncodingUtils.base64EncodeToString(
            tenantId +
                ":" +
                jobId +
                ":" +
                uuid +
                ":" +
                approved);
    }
}
