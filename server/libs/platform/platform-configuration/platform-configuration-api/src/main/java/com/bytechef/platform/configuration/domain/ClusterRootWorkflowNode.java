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

package com.bytechef.platform.configuration.domain;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.evaluator.Evaluator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class ClusterRootWorkflowNode {

    private final WorkflowTask workflowTask;
    private final WorkflowTrigger workflowTrigger;

    private ClusterRootWorkflowNode(WorkflowTask workflowTask, WorkflowTrigger workflowTrigger) {
        this.workflowTask = workflowTask;
        this.workflowTrigger = workflowTrigger;
    }

    public static ClusterRootWorkflowNode of(Workflow workflow, String workflowNodeName) {
        Optional<WorkflowTrigger> workflowTriggerOptional = WorkflowTrigger.fetch(workflow, workflowNodeName);

        if (workflowTriggerOptional.isPresent()) {
            return new ClusterRootWorkflowNode(null, workflowTriggerOptional.get());
        }

        return new ClusterRootWorkflowNode(workflow.getTask(workflowNodeName), null);
    }

    public static ClusterRootWorkflowNode of(WorkflowTask workflowTask) {
        return new ClusterRootWorkflowNode(workflowTask, null);
    }

    public static ClusterRootWorkflowNode of(WorkflowTrigger workflowTrigger) {
        return new ClusterRootWorkflowNode(null, workflowTrigger);
    }

    public Map<String, ?> evaluateParameters(Map<String, ?> context, Evaluator evaluator) {
        if (workflowTrigger != null) {
            return workflowTrigger.evaluateParameters(context, evaluator);
        }

        return workflowTask.evaluateParameters(context, evaluator);
    }

    public ClusterElementMap getClusterElementMap() {
        return ClusterElementMap.of(getExtensions());
    }

    public Map<String, ?> getExtensions() {
        if (workflowTrigger != null) {
            return workflowTrigger.getExtensions();
        }

        return workflowTask.getExtensions();
    }

    public String getName() {
        if (workflowTrigger != null) {
            return workflowTrigger.getName();
        }

        return workflowTask.getName();
    }

    public String getType() {
        if (workflowTrigger != null) {
            return workflowTrigger.getType();
        }

        return workflowTask.getType();
    }

    public boolean isTrigger() {
        return workflowTrigger != null;
    }
}
