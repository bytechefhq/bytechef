/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.automation.configuration.event;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;

@Component
public class ProjectWorkflowAfterSaveEventListener extends AbstractRelationalEventListener<Workflow> {

    private final ProjectFacade projectFacade;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowAfterSaveEventListener(ProjectFacade projectFacade, ProjectService projectService) {
        this.projectFacade = projectFacade;
        this.projectService = projectService;
    }

    @Override
    protected void onAfterSave(AfterSaveEvent<Workflow> afterSaveEvent) {
        Workflow workflow = afterSaveEvent.getEntity();

        projectService.fetchWorkflowProject(Validate.notNull(workflow.getId(), "id"))
            .ifPresent(project -> projectFacade.checkProjectStatus(project.getId()));
    }
}
