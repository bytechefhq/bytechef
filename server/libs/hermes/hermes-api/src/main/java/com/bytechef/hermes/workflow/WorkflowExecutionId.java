
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

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class WorkflowExecutionId {

    public static final String WORKFLOW_INSTANCE_ID = "workflowExecutionId";

    private final Long instanceId;
    private final long jobId;
    private final String triggerName;
    private final String workflowId;

    private WorkflowExecutionId(Long instanceId, long jobId, String triggerName, String workflowId) {
        this.instanceId = instanceId;
        this.jobId = jobId;
        this.triggerName = triggerName;
        this.workflowId = workflowId;
    }

    public static WorkflowExecutionId of(String workflowId, long jobId) {
        Assert.hasText(workflowId, "'workflowId' must not be null");

        return new WorkflowExecutionId(null, jobId, null, workflowId);
    }

    public static WorkflowExecutionId of(String workflowId, Long instanceId, long jobId) {
        Assert.hasText(workflowId, "'workflowId' must not be null");
        Assert.notNull(instanceId, "'instanceId' must not be null");

        return new WorkflowExecutionId(instanceId, jobId, null, workflowId);
    }

    public static WorkflowExecutionId of(String workflowId, Long instanceId, long jobId, String triggerName) {
        Assert.hasText(workflowId, "'workflowId' must not be null");
        Assert.notNull(instanceId, "'instanceId' must not be null");
        Assert.hasText(triggerName, "'triggerName' must not be null");

        return new WorkflowExecutionId(instanceId, jobId, triggerName, workflowId);
    }

    public static WorkflowExecutionId parse(String id) {
        id = Base64Utils.decodeToString(id);

        String[] items = id.split(":");

        return WorkflowExecutionId.of(
            items[0], StringUtils.hasText(items[1]) ? Long.parseLong(items[1]) : null, Long.parseLong(items[2]),
            items[3]);
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public Optional<Long> getInstanceId() {
        return Optional.ofNullable(instanceId);
    }

    public Optional<String> getTriggerName() {
        return Optional.ofNullable(triggerName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(workflowId);
        sb.append(':');
        sb.append(instanceId == null ? "" : instanceId);
        sb.append(':');
        sb.append(jobId);
        sb.append(':');
        sb.append(StringUtils.hasText(triggerName) ? triggerName : "");

        return Base64Utils.encodeToString(sb.toString());
    }
}
