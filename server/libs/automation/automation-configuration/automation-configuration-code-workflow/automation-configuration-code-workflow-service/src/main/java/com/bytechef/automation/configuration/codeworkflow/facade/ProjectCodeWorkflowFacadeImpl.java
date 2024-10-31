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

package com.bytechef.automation.configuration.codeworkflow.facade;

import com.bytechef.automation.configuration.codeworkflow.service.ProjectCodeWorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.constant.AppType;
import com.bytechef.workflow.ProjectHandler;
import com.bytechef.workflow.definition.ProjectDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProjectCodeWorkflowFacadeImpl implements ProjectCodeWorkflowFacade {

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowFacadeImpl(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService) {

        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
    }

    @Override
    public void save(long workspaceId, byte[] bytes, Language language) {
        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Project project = projectService.fetchProject(projectDefinition.getName())
            .map(curProject -> updateProject(curProject, projectDefinition))
            .orElseGet(() -> createProject(workspaceId, projectDefinition));

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(),
            language, bytes, AppType.AUTOMATION);

        projectCodeWorkflowService.create(codeWorkflowContainer, project);

        Map<String, String> workflowNameIds = codeWorkflowContainer.getWorkflowNameIds();

        for (Map.Entry<String, String> entry : workflowNameIds.entrySet()) {
            projectWorkflowService.addWorkflow(
                project.getId(), project.getLastProjectVersion(), entry.getValue(),
                EncodingUtils.base64EncodeToString(projectDefinition.getName() + '-' + entry.getKey()));
        }

        projectService.publishProject(project.getId(), null);
    }

    private Project createProject(long workspaceId, ProjectDefinition projectDefinition) {
        Project project = new Project();

        project.setDescription(OptionalUtils.orElse(projectDefinition.getDescription(), null));
        project.setName(projectDefinition.getName());
        project.setWorkspaceId(workspaceId);

        return projectService.create(project);
    }

    private ProjectDefinition loadProjectDefinition(Language language, byte[] bytes) throws IOException {
        Path path = Files.createTempFile("code_workflow_project", language.getExtension());

        Files.write(path, bytes);

        URI uri = path.toUri();

        try {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                uri.toURL(), language, uri.toString() + UUID.randomUUID());

            return projectHandler.getDefinition();
        } finally {
            Files.delete(path);
        }
    }

    private Project updateProject(Project project, ProjectDefinition projectDefinition) {
        project.setDescription(OptionalUtils.orElse(projectDefinition.getDescription(), null));

        return projectService.update(project);
    }
}
