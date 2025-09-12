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

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedWorkflowDTO;
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectWorkflowFacadeImpl implements ProjectWorkflowFacade {

    private final ComponentDefinitionHelper componentDefinitionHelper;
    private final EnvironmentService environmentService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final String publicUrl;
    private final SharedTemplateFileStorage sharedTemplateFileStorage;
    private final SharedTemplateService sharedTemplateService;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowFacadeImpl(
        ComponentDefinitionHelper componentDefinitionHelper, ApplicationProperties applicationProperties,
        EnvironmentService environmentService, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, SharedTemplateFileStorage sharedTemplateFileStorage,
        SharedTemplateService sharedTemplateService, WorkflowCacheManager workflowCacheManager,
        WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentDefinitionHelper = componentDefinitionHelper;
        this.environmentService = environmentService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.publicUrl = applicationProperties.getPublicUrl();
        this.sharedTemplateFileStorage = sharedTemplateFileStorage;
        this.sharedTemplateService = sharedTemplateService;
        this.workflowCacheManager = workflowCacheManager;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public ProjectWorkflow addWorkflow(long projectId, String definition) {
        Project project = projectService.getProject(projectId);

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return projectWorkflowService.addWorkflow(projectId, project.getLastProjectVersion(), workflow.getId());
    }

    @Override
    public void deleteSharedWorkflow(String workflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(projectWorkflow.getUuid());

        sharedTemplateFileStorage.deleteFile(sharedTemplate.getTemplate());

        sharedTemplate.setTemplate(null);

        sharedTemplateService.update(sharedTemplate);
    }

    @Override
    public void deleteWorkflow(String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(project.getId());

        for (ProjectDeployment projectDeployment : projectDeployments) {
            List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
                .getProjectDeploymentWorkflows(Validate.notNull(projectDeployment.getId(), "id"));

            if (CollectionUtils.anyMatch(
                projectDeploymentWorkflows,
                projectDeploymentWorkflow -> Objects.equals(projectDeploymentWorkflow.getWorkflowId(), workflowId))) {

                projectDeploymentWorkflows.stream()
                    .filter(
                        projectDeploymentWorkflow -> Objects.equals(
                            projectDeploymentWorkflow.getWorkflowId(), workflowId))
                    .findFirst()
                    .ifPresent(
                        projectDeploymentWorkflow -> projectDeploymentWorkflowService.delete(
                            projectDeploymentWorkflow.getId()));
            }
        }

        for (ProjectVersion projectVersion : project.getProjectVersions()) {
            projectWorkflowService.delete(project.getId(), projectVersion.getVersion(), workflowId);
        }

        workflowTestConfigurationService.delete(workflowId);

        workflowService.delete(workflowId);
    }

    @Override
    public String duplicateWorkflow(long projectId, String workflowId) {
        Project project = projectService.getWorkflowProject(workflowId);

        Workflow workflow = workflowService.duplicateWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", MapUtils.getString(definitionMap, "label", "(2)") + " (2)");

        workflowService.update(
            Validate.notNull(workflow.getId(), "id"), JsonUtils.writeWithDefaultPrettyPrinter(definitionMap),
            workflow.getVersion());

        projectWorkflowService.addWorkflow(projectId, project.getLastProjectVersion(), workflow.getId());

        return workflow.getId();
    }

    @Override
    public void exportSharedWorkflow(String workflowId, String description) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            ZipEntry workflowZipEntry = new ZipEntry("workflow.json");

            zipOutputStream.putNextEntry(workflowZipEntry);

            Workflow workflow = workflowService.getWorkflow(workflowId);

            String workflowJson = JsonUtils.write(workflow.getDefinition());

            zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            ZipEntry templateZipEntry = new ZipEntry("template.json");

            zipOutputStream.putNextEntry(templateZipEntry);

            ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

            String templateJson = JsonUtils.write(new Template(description, projectWorkflow.getProjectVersion()));

            zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();
            zipOutputStream.finish();

            byte[] zipBytes = byteArrayOutputStream.toByteArray();

            String fileName = "workflow_" + projectWorkflow.getUuid() + ".zip";

            FileEntry fileEntry = sharedTemplateFileStorage.storeFileContent(
                fileName, new ByteArrayInputStream(zipBytes));

            sharedTemplateService.save(projectWorkflow.getUuid(), fileEntry);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export shared workflow", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectWorkflowDTO getProjectWorkflow(String workflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        return new ProjectWorkflowDTO(workflowFacade.getWorkflow(workflowId), projectWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectWorkflows() {
        return projectWorkflowService.getProjectWorkflows()
            .stream()
            .map(projectWorkflow -> workflowFacade.fetchWorkflow(projectWorkflow.getWorkflowId())
                .map(workflowDTO -> new ProjectWorkflowDTO(workflowDTO, projectWorkflow))
                .orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectWorkflows(long projectId) {
        Project project = projectService.getProject(projectId);

        return projectWorkflowService
            .getProjectWorkflows(project.getId(), project.getLastProjectVersion())
            .stream()
            .map(projectWorkflow -> new ProjectWorkflowDTO(
                workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .sorted(
                (projectWorkflow1, projectWorkflow2) -> {
                    String label1 = projectWorkflow1.getLabel();
                    String label2 = projectWorkflow2.getLabel();

                    return label1.compareToIgnoreCase(label2);
                })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectVersionWorkflows(
        long projectId, int projectVersion, boolean includeAllFields) {

        if (includeAllFields) {
            return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
                .stream()
                .map(projectWorkflow -> new ProjectWorkflowDTO(
                    workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
                .toList();
        } else {
            return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
                .stream()
                .map(projectWorkflow -> new ProjectWorkflowDTO(
                    workflowService.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
                .toList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SharedWorkflowDTO getSharedWorkflow(String workflowUuid) {
        SharedWorkflowDTO sharedWorkflowDTO = null;

        Optional<SharedTemplate> sharedTemplateOptional = sharedTemplateService.fetchSharedTemplate(
            UUID.fromString(workflowUuid));

        if (sharedTemplateOptional.isPresent()) {
            SharedTemplate sharedTemplate = sharedTemplateOptional.get();

            if (sharedTemplate.getTemplate() == null) {
                sharedWorkflowDTO = new SharedWorkflowDTO(false);
            } else {
                TemplateFiles templateFiles = readTemplate(sharedTemplate.getTemplate());

                Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

                sharedWorkflowDTO =
                    new SharedWorkflowDTO(template.description, true, template.projectVersion, publicUrl);
            }
        }

        return sharedWorkflowDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowTemplateDTO getWorkflowTemplate(String id, boolean sharedWorkflow) {
        WorkflowTemplateDTO workflowTemplateDTO = null;

        if (sharedWorkflow) {
            SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(
                UUID.fromString(id));

            if (sharedTemplate.getTemplate() == null) {
                throw new IllegalStateException("Shared template is not available");
            }

            TemplateFiles templateFiles = readTemplate(sharedTemplate.getTemplate());

            Template template = JsonUtils.read(templateFiles.templateJson, Template.class);
            String definition = JsonUtils.read(templateFiles.workflowJson, String.class);

            Workflow workflow = new Workflow(definition, Workflow.Format.JSON);

            List<ComponentDefinition> componentDefinitions = componentDefinitionHelper.getComponentDefinitions(
                workflow);

            workflowTemplateDTO = new WorkflowTemplateDTO(
                componentDefinitions, template.description, true, template.projectVersion, publicUrl,
                new WorkflowTemplateDTO.WorkflowInfo(workflow.getLabel(), workflow.getDescription()));
        }

        return workflowTemplateDTO;
    }

    @Override
    public long importWorkflowTemplate(long projectId, String workflowUuid, boolean sharedWorkflow) {
        long projectWorkflowId;

        if (sharedWorkflow) {
            SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(workflowUuid));

            TemplateFiles templateFiles = readTemplate(sharedTemplate.getTemplate());

            String definition = JsonUtils.read(templateFiles.workflowJson, String.class);

            ProjectWorkflow projectWorkflow = addWorkflow(projectId, definition);

            projectWorkflowId = projectWorkflow.getId();
        } else {
            projectWorkflowId = 0;
        }

        return projectWorkflowId;
    }

    @Override
    public void updateWorkflow(String workflowId, String definition, int version) {
        workflowService.update(workflowId, definition, version);

        for (String cacheName : WorkflowNodeOutputFacade.WORKFLOW_CACHE_NAMES) {
            for (Environment environment : environmentService.getEnvironments()) {
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName, environment.ordinal());
            }
        }
    }

    private TemplateFiles readTemplate(FileEntry fileEntry) {
        String templateJson = null;
        String workflowJson = null;

        try (InputStream inputStream = sharedTemplateFileStorage.getFileStream(fileEntry);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();

                if ("template.json".equals(name)) {
                    templateJson = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                }

                if ("workflow.json".equals(name)) {
                    workflowJson = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                }

                zipInputStream.closeEntry();
            }

            if (templateJson == null || workflowJson == null) {
                throw new RuntimeException("Missing files in a shared workflow file");
            }

            return new TemplateFiles(templateJson, workflowJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to import shared workflow", e);
        }
    }

    private record Template(
        String description, List<String> categories, String authorName, String authorRole, String authorEmail,
        String authorSocialLinks, Instant lastModifiedDate, Integer projectVersion) {

        public Template(String description, int lastProjectVersion) {
            this(description, Collections.emptyList(), null, null, null, null, Instant.now(), lastProjectVersion);
        }
    }

    private record TemplateFiles(String templateJson, String workflowJson) {
    }
}
