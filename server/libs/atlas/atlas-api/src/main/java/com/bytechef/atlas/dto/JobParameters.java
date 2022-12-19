
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

    private String jobId;

    private Map<String, Object> inputs = Collections.emptyMap();

    private String label;

    private Map<String, Object> outputs = Collections.emptyMap();

    private String parentTaskExecutionId;

    private Integer priority;

    private String workflowId;

    private List<Map<String, Object>> webhooks = Collections.emptyList();

    public Map<String, Object> getInputs() {
        return new HashMap<>(inputs);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setInputs(Map<String, Object> inputs) {
        if (inputs != null) {
            this.inputs = new HashMap<>(inputs);
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Object> getOutputs() {
        return new HashMap<>(outputs);
    }

    public void setOutputs(Map<String, Object> outputs) {
        if (outputs != null) {
            this.outputs = new HashMap<>(outputs);
        }
    }

    public String getParentTaskExecutionId() {
        return parentTaskExecutionId;
    }

    public void setParentTaskExecutionId(String parentTaskExecutionId) {
        this.parentTaskExecutionId = parentTaskExecutionId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public List<Map<String, Object>> getWebhooks() {
        return new ArrayList<>(webhooks);
    }

    public void setWebhooks(List<Map<String, Object>> webhooks) {
        if (webhooks != null) {
            this.webhooks = new ArrayList<>(webhooks);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobParameters workflowParameters = (JobParameters) o;

        return Objects.equals(this.jobId, workflowParameters.jobId)
            && Objects.equals(this.inputs, workflowParameters.inputs)
            && Objects.equals(this.label, workflowParameters.label)
            && Objects.equals(this.outputs, workflowParameters.outputs)
            && Objects.equals(this.parentTaskExecutionId, workflowParameters.parentTaskExecutionId)
            && Objects.equals(this.priority, workflowParameters.priority)
            && Objects.equals(this.workflowId, workflowParameters.workflowId)
            && Objects.equals(this.webhooks, workflowParameters.webhooks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, inputs, label, outputs, parentTaskExecutionId, priority, workflowId, webhooks);
    }

    @Override
    public String toString() {
        return "WorkflowParametersDTO{" + "inputs="
            + inputs + ", label='"
            + label + '\'' + ", outputs="
            + outputs + ", parentTaskExecutionId='"
            + parentTaskExecutionId + '\'' + ", priority="
            + priority + ", workflowId='"
            + workflowId + '\'' + ", webhooks="
            + webhooks + '}';
    }
}
