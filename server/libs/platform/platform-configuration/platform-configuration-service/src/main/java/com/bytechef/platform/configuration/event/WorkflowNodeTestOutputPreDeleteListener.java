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

package com.bytechef.platform.configuration.event;

import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Removes the node test outputs recorded for a workflow before that workflow is deleted.
 * <p>
 * Test outputs are keyed on the workflow id alone, so nothing removes them when the workflow row goes. Left behind they
 * accumulate forever, and a workflow later created with the same id would read them back as its own.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowNodeTestOutputPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNodeTestOutputPreDeleteListener.class);

    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputPreDeleteListener(WorkflowNodeTestOutputService workflowNodeTestOutputService) {
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        log.debug("Cleaning up node test outputs for workflow {}", workflowId);

        workflowNodeTestOutputService.deleteWorkflowNodeTestOutputs(workflowId);
    }
}
