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
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
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
public class SharedWorkflowGraphQlController {

    private final ProjectWorkflowFacade projectWorkflowFacade;

    public SharedWorkflowGraphQlController(ProjectWorkflowFacade projectWorkflowFacade) {
        this.projectWorkflowFacade = projectWorkflowFacade;
    }

    @MutationMapping
    public String exportSharedWorkflow(@Argument String workflowId, @Argument String templateDescription) {
        return projectWorkflowFacade.exportSharedWorkflow(workflowId, templateDescription);
    }

    @QueryMapping
    public SharedWorkflowDTO getSharedWorkflow(@Argument String workflowUuid) {
        return projectWorkflowFacade.getSharedWorkflow(workflowUuid);
    }

    @MutationMapping
    public Boolean importSharedWorkflow(@Argument String workflowUuid, @Argument Long projectId) {
        projectWorkflowFacade.importSharedWorkflow(workflowUuid, projectId);

        return true;
    }
}
