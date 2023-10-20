
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

package com.bytechef.hermes.trigger;

import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.workflow.WorkflowExecutionId;

import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class TriggerExecution implements Errorable, Trigger {

    private TriggerExecution() {
    }

    public TriggerExecution(Object output, WorkflowExecutionId workflowExecutionId) {
        this.output = output;
        this.workflowExecutionId = workflowExecutionId;
    }

    public enum Status {
        CREATED, STARTED, FAILED, CANCELLED, COMPLETED
    }

    private WorkflowExecutionId workflowExecutionId;
    private WorkflowTrigger workflowTrigger;
    private Object output;
    private ExecutionError error;
    private Status status = Status.CREATED;
    private String timeout;
    private String type;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TriggerExecution that = (TriggerExecution) o;

        return Objects.equals(workflowExecutionId, that.workflowExecutionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workflowExecutionId);
    }

    public Map<String, Object> getParameters() {
        return workflowTrigger.parameters();
    }

    public WorkflowExecutionId getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public WorkflowTrigger getWorkflowTrigger() {
        return workflowTrigger;
    }

    @Override
    public ExecutionError getError() {
        return error;
    }

    public Object getOutput() {
        return output;
    }

    public Status getStatus() {
        return status;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public String getTimeout() {
        return timeout;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setWorkflowExecutionId(WorkflowExecutionId workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public void setWorkflowTrigger(WorkflowTrigger workflowTrigger) {
        this.workflowTrigger = workflowTrigger;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    public void setType(String type) {
        this.type = type;
    }
}
