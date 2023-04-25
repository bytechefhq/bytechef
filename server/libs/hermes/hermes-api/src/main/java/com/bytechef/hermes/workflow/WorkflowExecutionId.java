
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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId {

    private final long instanceId;
    private final String instanceType;
    private final String triggerName;
    private final String workflowId;

    private WorkflowExecutionId(long instanceId, String instanceType, String triggerName, String workflowId) {
        this.instanceId = instanceId;
        this.instanceType = instanceType;
        this.triggerName = triggerName;
        this.workflowId = workflowId;
    }

    public static WorkflowExecutionId of(String workflowId, long instanceId, String instanceType, String triggerName) {
        Assert.hasText(workflowId, "'workflowId' must not be null");
        Assert.notNull(instanceType, "'instanceType' must not be null");
        Assert.hasText(triggerName, "'triggerName' must not be null");

        return new WorkflowExecutionId(instanceId, instanceType, triggerName, workflowId);
    }

    public static WorkflowExecutionId parse(String id) {
        id = Base64Utils.decodeToString(id);

        String[] items = id.split(":");

        return WorkflowExecutionId.of(
            items[0], Long.parseLong(items[1]), StringUtils.hasText(items[2]) ? items[2] : null, items[3]);
    }

    public String getWorkflowId() {
        return workflowId;
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

    @Override
    public String toString() {
        String id = workflowId +
            ':' +
            instanceId +
            ':' +
            (instanceType == null ? "" : instanceType) +
            ':' +
            (StringUtils.hasText(triggerName) ? triggerName : "");

        return Base64Utils.encodeToString(id);
    }

    public record Instance(long id, String type) {
    }
}
