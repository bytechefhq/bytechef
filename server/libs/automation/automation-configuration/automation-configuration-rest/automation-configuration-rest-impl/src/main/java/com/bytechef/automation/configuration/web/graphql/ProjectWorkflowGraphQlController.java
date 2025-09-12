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

package com.bytechef.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing shared workflows in the automation configuration module.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class ProjectWorkflowGraphQlController {

    private final ProjectWorkflowFacade projectWorkflowFacade;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowGraphQlController(ProjectWorkflowFacade projectWorkflowFacade) {
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @MutationMapping
    public Boolean deleteSharedWorkflow(@Argument String workflowId) {
        projectWorkflowFacade.deleteSharedWorkflow(workflowId);

        return true;
    }

    @MutationMapping
    public Boolean exportSharedWorkflow(@Argument String workflowId, @Argument String description) {
        projectWorkflowFacade.exportSharedWorkflow(workflowId, description);

        return true;
    }

    @MutationMapping
    public Long importWorkflowTemplate(
        @Argument String id, @Argument Long projectId, @Argument boolean sharedWorkflow) {

        return projectWorkflowFacade.importWorkflowTemplate(projectId, id, sharedWorkflow);
    }

    @QueryMapping(name = "preBuiltWorkflowTemplates")
    public List<WorkflowTemplateDTO> preBuiltWorkflowTemplates(@Argument String query, @Argument String category) {
        return projectWorkflowFacade.getPreBuiltWorkflowTemplates(query, category);
    }

    @QueryMapping(name = "sharedWorkflow")
    public SharedWorkflowDTO sharedWorkflow(@Argument String workflowUuid) {
        return projectWorkflowFacade.getSharedWorkflow(workflowUuid);
    }

    @QueryMapping(name = "workflowTemplate")
    public WorkflowTemplateDTO workflowTemplate(@Argument String id, @Argument boolean sharedWorkflow) {
        return projectWorkflowFacade.getWorkflowTemplate(id, sharedWorkflow);
    }
}
