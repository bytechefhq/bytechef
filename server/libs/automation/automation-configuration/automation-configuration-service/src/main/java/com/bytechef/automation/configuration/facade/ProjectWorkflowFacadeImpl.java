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
import com.bytechef.automation.configuration.dto.WorkflowTemplateDTO.WorkflowInfo;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
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
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.githubproxy.client.WorkflowTemplate;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateAuthor;
import com.bytechef.platform.githubproxy.client.WorkflowTemplateSummary;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectWorkflowFacadeImpl implements ProjectWorkflowFacade {

    private static final Logger log = LoggerFactory.getLogger(ProjectWorkflowFacadeImpl.class);

    private final ComponentDefinitionHelper componentDefinitionHelper;
    private final EnvironmentService environmentService;
    private final PreBuiltTemplateService preBuiltTemplateService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final String publicUrl;
    private final SharedTemplateFileStorage sharedTemplateFileStorage;
    private final SharedTemplateService sharedTemplateService;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowFacade workflowFacade;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public ProjectWorkflowFacadeImpl(
        ComponentDefinitionHelper componentDefinitionHelper, PreBuiltTemplateService preBuiltTemplateService,
        ApplicationProperties applicationProperties, EnvironmentService environmentService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, SharedTemplateFileStorage sharedTemplateFileStorage,
        SharedTemplateService sharedTemplateService, WorkflowCacheManager workflowCacheManager,
        WorkflowFacade workflowFacade, List<WorkflowPreDeleteListener> workflowPreDeleteListeners,
        WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentDefinitionHelper = componentDefinitionHelper;
        this.preBuiltTemplateService = preBuiltTemplateService;
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
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
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

        for (WorkflowPreDeleteListener listener : workflowPreDeleteListeners) {
            listener.onWorkflowPreDelete(workflowId);
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

            ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

            ZipEntry workflowZipEntry = new ZipEntry(String.format("workflow-%s.json", projectWorkflow.getUuid()));

            zipOutputStream.putNextEntry(workflowZipEntry);

            Workflow workflow = workflowService.getWorkflow(workflowId);

            String definition = workflow.getDefinition();

            zipOutputStream.write(definition.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            ZipEntry templateZipEntry = new ZipEntry("template.json");

            zipOutputStream.putNextEntry(templateZipEntry);

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
    public List<WorkflowTemplateDTO> getPreBuiltWorkflowTemplates(String query, String category) {
        return preBuiltTemplateService.getWorkflowTemplates()
            .stream()
            .map(workflowTemplateSummary -> {
                try {
                    return toWorkflowTemplateDTO(workflowTemplateSummary);
                } catch (Exception e) {
                    log.error("Failed to map workflow template '{}'", workflowTemplateSummary.slug(), e);

                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(workflowTemplateDTO -> matchesQueryAndCategory(workflowTemplateDTO, query, category))
            .toList();
    }

    private static boolean matchesQueryAndCategory(
        WorkflowTemplateDTO workflowTemplateDTO, String query, String category) {

        if (StringUtils.isEmpty(query) && StringUtils.isEmpty(category)) {
            return true;
        }

        if (StringUtils.isNotEmpty(query)) {
            WorkflowTemplateDTO.WorkflowInfo workflow = workflowTemplateDTO.workflow();

            if (Strings.CI.contains(workflowTemplateDTO.description(), query) ||
                Strings.CI.contains(workflow.label(), query) ||
                Strings.CI.contains(workflow.description(), query)) {

                return true;
            }
        }

        if (StringUtils.isNotEmpty(category)) {
            List<String> categories = workflowTemplateDTO.categories();

            return categories != null && categories.contains(category);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectWorkflowDTO getProjectWorkflow(long projectWorkflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(projectWorkflowId);

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(projectWorkflow.getWorkflowId());

        return new ProjectWorkflowDTO(workflowDTO, projectWorkflow, workflowFacade.hasSseStreamResponse(workflowDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectWorkflowDTO getProjectWorkflow(String workflowId) {
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(workflowId);

        return new ProjectWorkflowDTO(workflowDTO, projectWorkflow, workflowFacade.hasSseStreamResponse(workflowDTO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectWorkflowDTO> getProjectWorkflows() {
        return projectWorkflowService.getProjectWorkflows()
            .stream()
            .map(projectWorkflow -> workflowFacade.fetchWorkflow(projectWorkflow.getWorkflowId())
                .map(workflowDTO -> new ProjectWorkflowDTO(
                    workflowDTO, projectWorkflow, workflowFacade.hasSseStreamResponse(workflowDTO)))
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
            .map(projectWorkflow -> {
                WorkflowDTO workflowDTO = workflowFacade.getWorkflow(projectWorkflow.getWorkflowId());

                return new ProjectWorkflowDTO(
                    workflowDTO, projectWorkflow, workflowFacade.hasSseStreamResponse(workflowDTO));
            })
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
                .map(projectWorkflow -> {
                    WorkflowDTO workflowDTO = workflowFacade.getWorkflow(projectWorkflow.getWorkflowId());

                    return new ProjectWorkflowDTO(
                        workflowDTO, projectWorkflow, workflowFacade.hasSseStreamResponse(workflowDTO));
                })
                .toList();
        } else {
            return projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
                .stream()
                .map(projectWorkflow -> new ProjectWorkflowDTO(
                    workflowService.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow, false))
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
                try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                    TemplateFiles templateFiles = readTemplate(inputStream.readAllBytes());

                    Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

                    sharedWorkflowDTO = new SharedWorkflowDTO(
                        template.description, true, template.projectVersion, publicUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to import shared project", e);
                }
            }
        }

        return sharedWorkflowDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowTemplateDTO getWorkflowTemplate(String id, boolean sharedWorkflow) {
        if (!sharedWorkflow) {
            return toWorkflowTemplateDTO(preBuiltTemplateService.getWorkflowTemplate(id));
        }

        SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(id));

        if (sharedTemplate.getTemplate() == null) {
            throw new IllegalStateException("Shared template is not available");
        }

        byte[] data;

        try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
            data = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to import shared project", e);
        }

        TemplateFiles templateFiles = readTemplate(data);

        Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

        Workflow workflow = new Workflow(templateFiles.workflowJson, Workflow.Format.JSON);

        List<ComponentDefinition> componentDefinitions = componentDefinitionHelper.getComponentDefinitions(
            workflow);

        List<String> categories = template.categories == null || template.categories.isEmpty()
            ? List.of("other") : template.categories;

        return new WorkflowTemplateDTO(
            template.authorName, template.authorEmail, template.authorRole, template.authorSocialLinks,
            categories, componentDefinitions, template.description, id, template.lastModifiedDate,
            template.projectVersion, publicUrl, new WorkflowInfo(workflow.getLabel(), workflow.getDescription()));
    }

    @Override
    public long importWorkflowTemplate(long projectId, String id, boolean sharedWorkflow) {
        if (!sharedWorkflow) {
            WorkflowTemplate workflowTemplate = preBuiltTemplateService.getWorkflowTemplate(id);

            ProjectWorkflow projectWorkflow = addWorkflow(
                projectId, workflowTemplate.workflowDefinition()
                    .toString());

            return projectWorkflow.getId();
        }

        SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(id));

        byte[] data;

        try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
            data = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to import shared project", e);
        }

        TemplateFiles templateFiles = readTemplate(data);

        ProjectWorkflow projectWorkflow = addWorkflow(projectId, templateFiles.workflowJson);

        return projectWorkflow.getId();
    }

    @Override
    public ProjectWorkflowDTO updateWorkflow(String workflowId, String definition, int version) {
        workflowService.update(workflowId, definition, version);

        for (String cacheName : WorkflowNodeOutputFacade.WORKFLOW_CACHE_NAMES) {
            for (Environment environment : environmentService.getEnvironments()) {
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName, environment.ordinal());
            }
        }

        return getProjectWorkflow(workflowId);
    }

    private WorkflowTemplateDTO toWorkflowTemplateDTO(WorkflowTemplate workflowTemplate) {
        Workflow workflow = new Workflow(String.valueOf(workflowTemplate.workflowDefinition()), Workflow.Format.JSON);

        WorkflowTemplateAuthor author = workflowTemplate.author();

        return new WorkflowTemplateDTO(
            author == null ? "" : author.name(), author == null ? "" : author.email(),
            author == null ? "" : author.role(), author == null ? "" : author.socialLinks(),
            toCategories(workflowTemplate.category()), componentDefinitionHelper.getComponentDefinitions(workflow),
            workflowTemplate.description(), workflowTemplate.slug(), null, null, publicUrl,
            new WorkflowInfo(workflow.getLabel(), workflow.getDescription()));
    }

    private WorkflowTemplateDTO toWorkflowTemplateDTO(WorkflowTemplateSummary workflowTemplateSummary) {
        return new WorkflowTemplateDTO(
            "", "", "", "", toCategories(workflowTemplateSummary.category()),
            componentDefinitionHelper.getComponentDefinitions(workflowTemplateSummary.components()),
            workflowTemplateSummary.description(), workflowTemplateSummary.slug(), null, null, publicUrl,
            new WorkflowInfo(workflowTemplateSummary.title(), workflowTemplateSummary.description()));
    }

    private static List<String> toCategories(String category) {
        return category == null || category.isBlank() ? List.of("other") : List.of(category);
    }

    private TemplateFiles readTemplate(byte[] data) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(data))) {
            String templateJson = null;
            String workflowJson = null;
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String name = zipEntry.getName();

                if ("template.json".equals(name)) {
                    templateJson = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);
                } else if (name.startsWith("workflow-") && name.endsWith(".json")) {
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
        String authorName, String authorRole, String authorEmail, String authorSocialLinks, List<String> categories,
        String description, Instant lastModifiedDate, Integer projectVersion) {

        public Template(String description, int lastProjectVersion) {
            this(null, null, null, null, Collections.emptyList(), description, Instant.now(), lastProjectVersion);
        }
    }

    private record TemplateFiles(String templateJson, String workflowJson) {
    }
}
