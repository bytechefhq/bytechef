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

package com.bytechef.platform.workflow;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantIdValidator;
import java.io.Serializable;
import java.util.Objects;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private long jobPrincipalId;
    private PlatformType type;
    private String workflowUuid;
    private String tenantId;
    private String triggerName;

    private WorkflowExecutionId() {
        // Required by Jackson deserialization
    }

    private WorkflowExecutionId(
        String tenantId, PlatformType type, long jobPrincipalId, String workflowUuid, String triggerName) {

        this.jobPrincipalId = jobPrincipalId;
        this.tenantId = tenantId;
        this.triggerName = triggerName;
        this.type = type;
        this.workflowUuid = workflowUuid;
    }

    public static WorkflowExecutionId of(
        PlatformType type, long jobPrincipalId, String workflowUuid, String triggerName) {

        Assert.hasText(workflowUuid, "'workflowUuid' must not be blank");
        Assert.hasText(triggerName, "'triggerName' must not be blank");

        return new WorkflowExecutionId(
            TenantContext.getCurrentTenantId(), type, jobPrincipalId, workflowUuid, triggerName);
    }

    /**
     * @throws IllegalArgumentException if the id is not a valid encoded workflow execution id
     */
    public static WorkflowExecutionId parse(String id) {
        Assert.hasText(id, "'id' must not be blank");

        String decoded = EncodingUtils.base64DecodeToString(id);

        String[] items = decoded.split(":");

        if (items.length != 5) {
            throw new IllegalArgumentException("Invalid WorkflowExecutionId format");
        }

        String tenantId = TenantIdValidator.validate(items[0]);

        PlatformType[] platformTypes = PlatformType.values();

        int typeOrdinal = Integer.parseInt(items[1]);

        if (typeOrdinal < 0 || typeOrdinal >= platformTypes.length) {
            throw new IllegalArgumentException("Invalid WorkflowExecutionId type");
        }

        Assert.hasText(items[3], "'workflowUuid' must not be blank");
        Assert.hasText(items[4], "'triggerName' must not be blank");

        return new WorkflowExecutionId(
            tenantId, platformTypes[typeOrdinal], Long.parseLong(items[2]), items[3], items[4]);
    }

    public long getJobPrincipalId() {
        return jobPrincipalId;
    }

    public PlatformType getType() {
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
        PlatformType type1 = Objects.requireNonNull(type);

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
