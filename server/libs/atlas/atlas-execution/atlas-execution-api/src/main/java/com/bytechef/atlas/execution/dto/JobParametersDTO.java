/*
 * Copyright 2025 ByteChef
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

package com.bytechef.atlas.execution.dto;

import com.bytechef.atlas.execution.domain.Job.Webhook;
import com.bytechef.message.Prioritizable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class JobParametersDTO {

    private Map<String, ?> inputs = Collections.emptyMap();
    private String label;
    private Map<String, ?> metadata = Collections.emptyMap();
    private Long parentTaskExecutionId;
    private int priority = Prioritizable.DEFAULT_PRIORITY;
    private List<Webhook> webhooks = Collections.emptyList();
    private String workflowId;

    private JobParametersDTO() {
    }

    public JobParametersDTO(String workflowId) {
        this(workflowId, Map.of());
    }

    public JobParametersDTO(String workflowId, Map<String, ?> inputs) {
        this(workflowId, inputs, Map.of());
    }

    public JobParametersDTO(String workflowId, Map<String, ?> inputs, Map<String, ?> metadata) {
        this(workflowId, null, inputs, null, null, List.of(), metadata);
    }

    public JobParametersDTO(String workflowId, Long parentTaskExecutionId, Map<String, ?> inputs) {
        this(workflowId, parentTaskExecutionId, inputs, null, null, List.of(), Map.of());
    }

    @Default
    public JobParametersDTO(
        String workflowId, Long parentTaskExecutionId, Map<String, ?> inputs, String label, Integer priority,
        List<Webhook> webhooks, Map<String, ?> metadata) {

        if (inputs != null) {
            this.inputs = new HashMap<>(inputs);
        }

        this.label = label;
        this.metadata = new HashMap<>(metadata);
        this.parentTaskExecutionId = parentTaskExecutionId;

        if (priority != null) {
            this.priority = priority;
        }

        this.webhooks = new ArrayList<>(webhooks);
        this.workflowId = workflowId;
    }

    public Map<String, Object> getInputs() {
        return Collections.unmodifiableMap(inputs);
    }

    public String getLabel() {
        return label;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public Long getParentTaskExecutionId() {
        return parentTaskExecutionId;
    }

    public Integer getPriority() {
        return priority;
    }

    public List<Webhook> getWebhooks() {
        return Collections.unmodifiableList(webhooks);
    }

    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobParametersDTO jobParametersDTO = (JobParametersDTO) o;

        return Objects.equals(this.inputs, jobParametersDTO.inputs)
            && Objects.equals(this.label, jobParametersDTO.label)
            && Objects.equals(this.parentTaskExecutionId, jobParametersDTO.parentTaskExecutionId)
            && Objects.equals(this.priority, jobParametersDTO.priority)
            && Objects.equals(this.webhooks, jobParametersDTO.webhooks)
            && Objects.equals(this.workflowId, jobParametersDTO.workflowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, label, parentTaskExecutionId, priority, webhooks, workflowId);
    }

    @Override
    public String toString() {
        return "JobParameters{" +
            "workflowId='" + workflowId + '\'' +
            ", inputs=" + inputs +
            ", label='" + label + '\'' +
            ", parentTaskExecutionId='" + parentTaskExecutionId + '\'' +
            ", priority=" + priority +
            ", webhooks=" + webhooks +
            '}';
    }

    /**
     * Used by MapStruct.
     */
    public @interface Default {
    }
}
