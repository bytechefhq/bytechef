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
import com.bytechef.platform.constant.AppType;
import com.bytechef.tenant.TenantContext;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private long instanceId;
    private AppType type;
    private String workflowReferenceCode;
    private String tenantId;
    private String triggerName;

    private WorkflowExecutionId() {
    }

    private WorkflowExecutionId(
        String tenantId, AppType type, long instanceId, String workflowReferenceCode, String triggerName) {

        this.instanceId = instanceId;
        this.tenantId = tenantId;
        this.triggerName = triggerName;
        this.type = type;
        this.workflowReferenceCode = workflowReferenceCode;
    }

    public static WorkflowExecutionId of(
        AppType type, long instanceId, String workflowReferenceCode, String triggerName) {

        Validate.notBlank(workflowReferenceCode, "'workflowReferenceCode' must not be null");
        Validate.notBlank(triggerName, "'workflowTriggerName' must not be null");

        return new WorkflowExecutionId(
            TenantContext.getCurrentTenantId(), type, instanceId, workflowReferenceCode, triggerName);
    }

    public static WorkflowExecutionId parse(String id) {
        id = EncodingUtils.decodeBase64ToString(id);

        String[] items = id.split(":");

        return new WorkflowExecutionId(
            items[0], AppType.values()[Integer.parseInt(items[1])], Long.parseLong(items[2]), items[3], items[4]);
    }

    public long getInstanceId() {
        return instanceId;
    }

    public AppType getType() {
        return type;
    }

    public String getWorkflowReferenceCode() {
        return workflowReferenceCode;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTriggerName() {
        return triggerName;
    }

    @Override
    public String toString() {
        return EncodingUtils.encodeBase64ToString(
            tenantId +
                ":" +
                type.ordinal() +
                ":" +
                instanceId +
                ":" +
                workflowReferenceCode +
                ":" +
                triggerName);
    }
}
