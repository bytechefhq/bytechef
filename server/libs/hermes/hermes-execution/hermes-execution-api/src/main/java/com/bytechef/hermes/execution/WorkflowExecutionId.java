
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

import com.bytechef.commons.util.EncodingUtils;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private long instanceId;
    private int instanceType;
    private String workflowId;
    private String triggerName;

    private WorkflowExecutionId() {
    }

    private WorkflowExecutionId(int instanceType, long instanceId, String workflowId, String triggerName) {
        this.instanceId = instanceId;
        this.instanceType = instanceType;
        this.triggerName = triggerName;
        this.workflowId = workflowId;
    }

    public static WorkflowExecutionId of(int instanceType, long instanceId, String workflowId, String triggerName) {
        Assert.hasText(workflowId, "'workflowId' must not be null");
        Assert.hasText(triggerName, "'workflowTriggerName' must not be null");

        return new WorkflowExecutionId(instanceType, instanceId, workflowId, triggerName);
    }

    public static WorkflowExecutionId parse(String id) {
        id = EncodingUtils.decodeBase64ToString(id);

        String[] items = id.split(":");

        return WorkflowExecutionId.of(Integer.parseInt(items[0]), Long.parseLong(items[1]), items[2], items[3]);
    }

    public long getInstanceId() {
        return instanceId;
    }

    public int getInstanceType() {
        return instanceType;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getTriggerName() {
        return triggerName;
    }

    @Override
    public String toString() {
        return EncodingUtils.encodeBase64ToString(
            instanceType +
                ":" +
                instanceId +
                ":" +
                workflowId +
                ":" +
                triggerName);
    }
}
