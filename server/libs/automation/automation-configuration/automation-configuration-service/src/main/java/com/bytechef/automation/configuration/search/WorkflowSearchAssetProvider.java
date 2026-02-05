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

package com.bytechef.automation.configuration.search;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class WorkflowSearchAssetProvider implements SearchAssetProvider {

    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    WorkflowSearchAssetProvider(ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public List<WorkflowSearchResult> search(String query, int limit) {
        String queryLower = query.toLowerCase(Locale.ROOT);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getLatestProjectWorkflows();

        if (projectWorkflows.isEmpty()) {
            return List.of();
        }

        List<String> workflowIds = projectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        List<Workflow> workflows = workflowService.getWorkflows(workflowIds);

        Map<String, Workflow> workflowIdToWorkflow = workflows.stream()
            .collect(Collectors.toMap(Workflow::getId, Function.identity()));

        return projectWorkflows.stream()
            .filter(projectWorkflow -> {
                Workflow workflow = workflowIdToWorkflow.get(projectWorkflow.getWorkflowId());

                return workflow != null &&
                    (containsIgnoreCase(workflow.getLabel(), queryLower) ||
                        containsIgnoreCase(workflow.getDescription(), queryLower));
            })
            .limit(limit)
            .map(projectWorkflow -> {
                Workflow workflow = workflowIdToWorkflow.get(projectWorkflow.getWorkflowId());

                return new WorkflowSearchResult(
                    projectWorkflow.getId(), projectWorkflow.getProjectId(),
                    workflow.getLabel() != null ? workflow.getLabel() : workflow.getId(), workflow.getDescription());
            })
            .toList();
    }

    @Override
    public SearchAssetType getAssetType() {
        return SearchAssetType.WORKFLOW;
    }

    private boolean containsIgnoreCase(String text, String query) {
        if (text == null) {
            return false;
        }

        return text.toLowerCase(Locale.ROOT)
            .contains(query);
    }
}
