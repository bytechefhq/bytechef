
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

package com.bytechef.atlas.dto;

import com.bytechef.atlas.domain.Job;
import com.bytechef.message.Prioritizable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class JobParameters {

    private Map<String, Object> inputs = Collections.emptyMap();
    private String label;
    private Long parentTaskExecutionId;
    private int priority = Prioritizable.DEFAULT_PRIORITY;
    private String workflowId;
    private List<Job.Webhook> webhooks = Collections.emptyList();

    private JobParameters() {
    }

    @SuppressFBWarnings("EI2")
    public JobParameters(Map<String, Object> inputs, Long parentTaskExecutionId, String workflowId) {
        this.inputs = inputs;
        this.parentTaskExecutionId = parentTaskExecutionId;
        this.workflowId = workflowId;
    }

    @SuppressFBWarnings("EI2")
    public JobParameters(Map<String, Object> inputs, String workflowId) {
        this.inputs = inputs;
        this.workflowId = workflowId;
    }

    @Default
    public JobParameters(
        Map<String, Object> inputs, String label, Long parentTaskExecutionId, Integer priority, String workflowId,
        List<Job.Webhook> webhooks) {

        if (inputs != null) {
            this.inputs = new HashMap<>(inputs);
        }

        this.label = label;

        this.parentTaskExecutionId = parentTaskExecutionId;

        if (priority != null) {
            this.priority = priority;
        }

        this.workflowId = workflowId;

        if (webhooks != null) {
            this.webhooks = new ArrayList<>(webhooks);
        }
    }

    public Map<String, Object> getInputs() {
        return new HashMap<>(inputs);
    }

    public String getLabel() {
        return label;
    }

    public Long getParentTaskExecutionId() {
        return parentTaskExecutionId;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public List<Job.Webhook> getWebhooks() {
        return new ArrayList<>(webhooks);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobParameters jobParameters = (JobParameters) o;

        return Objects.equals(this.inputs, jobParameters.inputs)
            && Objects.equals(this.label, jobParameters.label)
            && Objects.equals(this.parentTaskExecutionId, jobParameters.parentTaskExecutionId)
            && Objects.equals(this.priority, jobParameters.priority)
            && Objects.equals(this.workflowId, jobParameters.workflowId)
            && Objects.equals(this.webhooks, jobParameters.webhooks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, label, parentTaskExecutionId, priority, workflowId, webhooks);
    }

    @Override
    public String toString() {
        return "JobParameters{" +
            "inputs=" + inputs +
            ", label='" + label + '\'' +
            ", parentTaskExecutionId='" + parentTaskExecutionId + '\'' +
            ", priority=" + priority +
            ", workflowId='" + workflowId + '\'' +
            ", webhooks=" + webhooks +
            '}';
    }

    public @interface Default {
    }
}
