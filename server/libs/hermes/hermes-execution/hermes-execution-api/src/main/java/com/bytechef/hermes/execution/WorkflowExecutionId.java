
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.execution;

import com.bytechef.commons.util.Base64Utils;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private final String componentName;
    private final int componentVersion;
    private final String componentTriggerName;
    private final long instanceId;
    private final String instanceType;
    private final String workflowId;
    private final String workflowTriggerName;
    private final boolean webhookRawBody;
    private final boolean workflowSyncExecution;
    private final boolean workflowSyncValidation;

    private WorkflowExecutionId(
        String workflowId, long instanceId, String instanceType, String workflowTriggerName,
        String componentName, int componentVersion, String componentTriggerName, boolean webhookRawBody,
        boolean workflowSyncExecution, boolean workflowSyncValidation) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.instanceId = instanceId;
        this.instanceType = instanceType;
        this.componentTriggerName = componentTriggerName;
        this.workflowId = workflowId;
        this.workflowTriggerName = workflowTriggerName;
        this.webhookRawBody = webhookRawBody;
        this.workflowSyncExecution = workflowSyncExecution;
        this.workflowSyncValidation = workflowSyncValidation;
    }

    public static WorkflowExecutionId of(
        String workflowId, long instanceId, String instanceType, String workflowTriggerName,
        String componentName, int componentVersion, String componentTriggerName, boolean rawBody,
        boolean workflowSyncExecution, boolean workflowSyncValidation) {

        Assert.hasText(workflowId, "'workflowId' must not be null");
        Assert.notNull(instanceType, "'instanceType' must not be null");
        Assert.hasText(workflowTriggerName, "'workflowTriggerName' must not be null");
        Assert.hasText(componentName, "'componentName' must not be null");
        Assert.hasText(componentTriggerName, "'componentTriggerName' must not be null");

        return new WorkflowExecutionId(
            workflowId, instanceId, instanceType, workflowTriggerName, componentName, componentVersion,
            componentTriggerName, rawBody, workflowSyncExecution, workflowSyncValidation);
    }

    public static WorkflowExecutionId parse(String id) {
        id = Base64Utils.decodeToString(id);

        String[] items = id.split(":");

        return WorkflowExecutionId.of(
            items[0], Long.parseLong(items[1]), items[2], items[3], items[4], Integer.parseInt(items[5]), items[6],
            Boolean.parseBoolean(items[7]), Boolean.parseBoolean(items[8]), Boolean.parseBoolean(items[9]));
    }

    public boolean isWebhookRawBody() {
        return webhookRawBody;
    }

    public boolean isWorkflowSyncExecution() {
        return workflowSyncExecution;
    }

    public boolean isWorkflowSyncValidation() {
        return workflowSyncValidation;
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getComponentTriggerName() {
        return componentTriggerName;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowTriggerName() {
        return workflowTriggerName;
    }

    @Override
    public String toString() {
        return Base64Utils.encodeToString(
            workflowId +
                ':' +
                instanceId +
                ':' +
                instanceType +
                ':' +
                workflowTriggerName +
                ':' +
                componentName +
                ':' +
                componentVersion +
                ':' +
                componentTriggerName +
                ':' +
                workflowSyncExecution +
                ':' +
                workflowSyncValidation +
                ':' +
                webhookRawBody);
    }
}
