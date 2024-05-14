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
import com.bytechef.platform.constant.Type;
import java.io.Serializable;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId implements Serializable {

    private long instanceId;
    private Type type;
    private String workflowId;
    private String triggerName;

    private WorkflowExecutionId() {
    }

    private WorkflowExecutionId(Type type, long instanceId, String workflowId, String triggerName) {
        this.instanceId = instanceId;
        this.triggerName = triggerName;
        this.type = type;
        this.workflowId = workflowId;
    }

    public static WorkflowExecutionId of(Type type, long instanceId, String workflowId, String triggerName) {
        Validate.notBlank(workflowId, "'workflowId' must not be null");
        Validate.notBlank(triggerName, "'workflowTriggerName' must not be null");

        return new WorkflowExecutionId(type, instanceId, workflowId, triggerName);
    }

    public static WorkflowExecutionId parse(String id) {
        id = EncodingUtils.decodeBase64ToString(id);

        String[] items = id.split(":");

        return of(Type.values()[Integer.parseInt(items[0])], Long.parseLong(items[1]), items[2], items[3]);
    }

    public long getInstanceId() {
        return instanceId;
    }

    public Type getType() {
        return type;
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
            type.ordinal() +
                ":" +
                instanceId +
                ":" +
                workflowId +
                ":" +
                triggerName);
    }
}
