
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

package com.bytechef.hermes.workflow;

import com.bytechef.commons.util.Base64Utils;
import com.bytechef.hermes.workflow.trigger.WorkflowTrigger;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private final String componentName;
    private final int componentVersion;
    private final long instanceId;
    private final String instanceType;
    private final String triggerName;
    private final String workflowId;
    private final String workflowTriggerName;

    private WorkflowExecutionId(
        String workflowId, long instanceId, String instanceType, String workflowTriggerName, String triggerName,
        String componentName, int componentVersion) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.instanceId = instanceId;
        this.instanceType = instanceType;
        this.triggerName = triggerName;
        this.workflowId = workflowId;
        this.workflowTriggerName = workflowTriggerName;
    }

    public static WorkflowExecutionId of(
        String workflowId, long instanceId, String instanceType, WorkflowTrigger workflowTrigger) {

        return of(
            workflowId, instanceId, instanceType, workflowTrigger.getName(), workflowTrigger.getTriggerName(),
            workflowTrigger.getComponentName(), workflowTrigger.getComponentVersion());
    }

    public static WorkflowExecutionId of(
        String workflowId, long instanceId, String instanceType, String workflowTriggerName, String triggerName,
        String componentName, int componentVersion) {

        Assert.hasText(workflowId, "'workflowId' must not be null");
        Assert.notNull(instanceType, "'instanceType' must not be null");
        Assert.hasText(workflowTriggerName, "'workflowTriggerName' must not be null");
        Assert.hasText(triggerName, "'triggerName' must not be null");
        Assert.hasText(componentName, "'componentName' must not be null");

        return new WorkflowExecutionId(
            workflowId, instanceId, instanceType, workflowTriggerName, triggerName, componentName, componentVersion);
    }

    public static WorkflowExecutionId parse(String id) {
        id = Base64Utils.decodeToString(id);

        String[] items = id.split(":");

        return WorkflowExecutionId.of(
            items[0], Long.parseLong(items[1]), items[2], items[3], items[4], items[5], Integer.parseInt(items[6]));
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

    public String getTriggerName() {
        return triggerName;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowTriggerName() {
        return workflowTriggerName;
    }

    @Override
    public String toString() {
        String id = workflowId +
            ':' +
            instanceId +
            ':' +
            (instanceType == null ? "" : instanceType) +
            ':' +
            workflowTriggerName +
            ':' +
            triggerName +
            ':' +
            componentName +
            ':' +
            componentVersion;

        return Base64Utils.encodeToString(id);
    }
}
