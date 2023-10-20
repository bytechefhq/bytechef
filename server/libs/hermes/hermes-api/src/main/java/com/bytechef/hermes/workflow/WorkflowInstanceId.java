
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

import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class WorkflowInstanceId {

    public static final String WORKFLOW_INSTANCE_ID = "workflowInstanceId";

    private final String workflowId;
    private final Long instanceId;
    private final String triggerName;

    private WorkflowInstanceId(String workflowId, Long instanceId, String triggerName) {
        this.workflowId = workflowId;
        this.instanceId = instanceId;
        this.triggerName = triggerName;
    }

    public static WorkflowInstanceId of(String workflowId, Long instanceId) {
        Objects.requireNonNull(workflowId, "'workflowId' must not be null");
        Objects.requireNonNull(instanceId, "'instanceId' must not be null");

        return new WorkflowInstanceId(workflowId, instanceId, null);
    }

    public static WorkflowInstanceId of(String workflowId, Long instanceId, String triggerName) {
        Objects.requireNonNull(workflowId, "'workflowId' must not be null");
        Objects.requireNonNull(instanceId, "'instanceId' must not be null");
        Objects.requireNonNull(triggerName, "'triggerName' must not be null");

        return new WorkflowInstanceId(workflowId, instanceId, triggerName);
    }

    public static WorkflowInstanceId of(String id) {
        id = Base64Utils.decodeToString(id);

        String[] items = id.split(":");

        return new WorkflowInstanceId(items[0], Long.parseLong(items[1]), items.length == 2 ? null : items[2]);
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public Optional<String> getTriggerName() {
        return Optional.ofNullable(triggerName);
    }

    @Override
    public String toString() {
        return Base64Utils.encodeToString(workflowId + ":" + instanceId + ":" + triggerName);
    }
}
