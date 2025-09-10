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
import com.bytechef.platform.constant.ModeType;
import com.bytechef.tenant.TenantContext;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private long jobPrincipalId;

    @Nullable
    private ModeType type;

    @Nullable
    private String workflowUuid;

    @Nullable
    private String tenantId;

    @Nullable
    private String triggerName;

    private WorkflowExecutionId() {
        // Required by Jackson deserialization
    }

    private WorkflowExecutionId(
        String tenantId, ModeType type, long jobPrincipalId, String workflowUuid, String triggerName) {

        this.jobPrincipalId = jobPrincipalId;
        this.tenantId = tenantId;
        this.triggerName = triggerName;
        this.type = type;
        this.workflowUuid = workflowUuid;
    }

    public static WorkflowExecutionId of(
        ModeType type, long jobPrincipalId, String workflowUuid, String triggerName) {

        Assert.hasText(workflowUuid, "'workflowUuid' must not be blank");
        Assert.hasText(triggerName, "'triggerName' must not be blank");

        return new WorkflowExecutionId(
            TenantContext.getCurrentTenantId(), type, jobPrincipalId, workflowUuid, triggerName);
    }

    public static WorkflowExecutionId parse(String id) {
        id = EncodingUtils.base64DecodeToString(id);

        String[] items = id.split(":");

        return new WorkflowExecutionId(
            items[0], ModeType.values()[Integer.parseInt(items[1])], Long.parseLong(items[2]), items[3], items[4]);
    }

    public long getJobPrincipalId() {
        return jobPrincipalId;
    }

    public ModeType getType() {
        return Objects.requireNonNull(type);
    }

    public String getWorkflowUuid() {
        return Objects.requireNonNull(workflowUuid);
    }

    public String getTenantId() {
        return Objects.requireNonNull(tenantId);
    }

    public String getTriggerName() {
        return Objects.requireNonNull(triggerName);
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
                jobPrincipalId +
                ":" +
                workflowUuid +
                ":" +
                triggerName);
    }
}
