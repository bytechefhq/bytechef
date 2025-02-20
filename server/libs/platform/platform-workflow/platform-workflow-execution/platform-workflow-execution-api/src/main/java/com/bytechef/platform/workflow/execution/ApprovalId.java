/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.platform.constant.ModeType;
import com.bytechef.tenant.TenantContext;
import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class ApprovalId implements Serializable {

    @Nullable
    private String approvalName;

    private boolean approved;
    private long jobId;

    @Nullable
    private String tenantId;

    @Nullable
    private ModeType type;

    private ApprovalId() {
        // Required by Jackson deserialization
    }

    private ApprovalId(String tenantId, ModeType type, long jobId, String approvalName, boolean approved) {
        this.approvalName = approvalName;
        this.approved = approved;
        this.jobId = jobId;
        this.tenantId = tenantId;
        this.type = type;
    }

    public static ApprovalId of(ModeType type, long jobId, String approvalName, boolean approved) {
        Validate.notBlank(approvalName, "'approvalName' must not be blank");

        return new ApprovalId(TenantContext.getCurrentTenantId(), type, jobId, approvalName, approved);
    }

    public static ApprovalId parse(String id) {
        id = EncodingUtils.base64DecodeToString(id);

        String[] items = id.split(":");

        return new ApprovalId(
            items[0], ModeType.values()[Integer.parseInt(items[1])], Long.parseLong(items[2]), items[3],
            Boolean.parseBoolean(items[4]));
    }

    public String getApprovalName() {
        return Objects.requireNonNull(approvalName);
    }

    public long getJobId() {
        return jobId;
    }

    public ModeType getType() {
        return Objects.requireNonNull(type);
    }

    public String getTenantId() {
        return Objects.requireNonNull(tenantId);
    }

    public boolean isApproved() {
        return approved;
    }

    @Override
    public String toString() {
        ModeType type1 = Objects.requireNonNull(type);

        return EncodingUtils.base64EncodeToString(
            tenantId +
                ":" +
                type1.ordinal()
                +
                ":" +
                jobId +
                ":" +
                approvalName +
                ":" +
                approved);
    }
}
