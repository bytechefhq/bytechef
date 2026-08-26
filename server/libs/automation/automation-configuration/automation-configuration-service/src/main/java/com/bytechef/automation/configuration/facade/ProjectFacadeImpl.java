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
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO.ComponentDefinitionTuple;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO.WorkflowInfo;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import com.bytechef.automation.configuration.dto.WorkspaceProjectWorkflowDTO;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier.CodeWorkflowInfo;
import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier.CodeWorkflowSource;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
@Service
public class ProjectFacadeImpl implements ProjectFacade {

    private static final Logger log = LoggerFactory.getLogger(ProjectFacadeImpl.class);

    /**
     * Zip entry naming the exported code workflow source; the suffix is the language, so an import knows how to load it
     * back without a separate descriptor.
     */
    private static final String CODE_WORKFLOW_ENTRY_PREFIX = "code-workflow.";

    private final CategoryService categoryService;
    private final ComponentDefinitionHelper componentDefinitionHelper;
    private final boolean eeEdition;
    private final ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;
    private final PermissionService permissionService;
    private final PreBuiltTemplateService preBuiltTemplateService;
    private final ObjectProvider<ProjectCodeWorkflowInfoSupplier> projectCodeWorkflowInfoSupplierProvider;
    private final ProjectService projectService;
    private final ProjectVisibilityFilter projectVisibilityFilter;
    private final ProjectWorkflowService projectWorkflowService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final String publicUrl;
    private final SharedTemplateFileStorage sharedTemplateFileStorage;
    private final SharedTemplateService sharedTemplateService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI2"
    })
    public ProjectFacadeImpl(
        @Value("${bytechef.edition:CE}") String edition,
        ApplicationProperties applicationProperties, CategoryService categoryService,
        ComponentDefinitionHelper componentDefinitionHelper,
        ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator,
        PermissionService permissionService, PreBuiltTemplateService preBuiltTemplateService,
        ObjectProvider<ProjectCodeWorkflowInfoSupplier> projectCodeWorkflowInfoSupplierProvider,
        ProjectWorkflowService projectWorkflowService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectVisibilityFilter projectVisibilityFilter,
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectWorkflowFacade projectWorkflowFacade,
        SharedTemplateFileStorage sharedTemplateFileStorage, SharedTemplateService sharedTemplateService,
        TagService tagService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        validateEdition(edition);

        this.categoryService = categoryService;
        this.componentDefinitionHelper = componentDefinitionHelper;
        this.eeEdition = "EE".equalsIgnoreCase(edition);
        this.errorWorkflowConfigurationValidator = errorWorkflowConfigurationValidator;
        this.permissionService = permissionService;
        this.preBuiltTemplateService = preBuiltTemplateService;
        this.projectCodeWorkflowInfoSupplierProvider = projectCodeWorkflowInfoSupplierProvider;
        this.projectWorkflowService = projectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectVisibilityFilter = projectVisibilityFilter;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.publicUrl = applicationProperties.getPublicUrl();
        this.sharedTemplateFileStorage = sharedTemplateFileStorage;
        this.sharedTemplateService = sharedTemplateService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
    }

    @Override
    @PreAuthorize("hasPermission(#projectDTO.workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public long createProject(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        applyCreateVisibility(project, projectDTO.visibility());

        Category category = projectDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        List<Tag> tags = checkTags(projectDTO.tags());

        if (!tags.isEmpty()) {
            project.setTags(tags);
        }

        project = projectService.create(project);

        return project.getId();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'PROJECT_DELETE')")
    public void deleteProject(long id) {
        List<ProjectDeployment> projectDeployments = projectDeploymentService.getAllProjectDeployments(id);

        for (ProjectDeployment projectDeployment : projectDeployments) {
            projectDeploymentFacade.deleteProjectDeployment(projectDeployment.getId());
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(id);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(projectWorkflow.getWorkflowId());
            }
        }

        projectWorkflowService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getId)
                .toList());

        workflowService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getWorkflowId)
                .toList());

        workflowTestConfigurationService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getWorkflowId)
                .toList());

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'PROJECT_SETTINGS')")
    public void deleteSharedProject(long id) {
        Project project = projectService.getProject(id);

        SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(
            UUID.fromString(Objects.requireNonNull(project.getUuid())));

        sharedTemplateFileStorage.deleteFile(sharedTemplate.getTemplate());

        sharedTemplate.setTemplate(null);

        sharedTemplateService.update(sharedTemplate);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_VIEW')")
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        Project newProject = new Project();

        newProject.setName(generateName(project.getName()));
        newProject.setTagIds(project.getTagIds());
        newProject.setWorkspaceId(project.getWorkspaceId());

        Optional<CodeWorkflowSource> codeWorkflowSource = fetchCodeWorkflowSource(id);

        if (codeWorkflowSource.isPresent()) {
            // A code project's workflows are generated from its source and carry the source container's uuid, so
            // copying the workflow rows would leave the copy pointing at the original's code. Deploy the source into
            // the new project instead and let it generate its own.
            newProject = projectService.create(newProject);

            CodeWorkflowSource source = codeWorkflowSource.get();

            long newProjectId = Objects.requireNonNull(newProject.getId());

            deployCodeWorkflowSource(newProjectId, source.language(), source.source());

            return toProjectDTO(projectService.getProject(newProjectId));
        }

        List<String> workflowIds = copyWorkflowIds(
            projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastProjectVersion()));

        newProject = projectService.create(newProject);

        for (String workflowId : workflowIds) {
            projectWorkflowService.addWorkflow(newProject.getId(), newProject.getLastProjectVersion(), workflowId);
        }

        return toProjectDTO(newProject);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_VIEW')")
    public byte[] exportProject(long id) {
        return createTemplate(id, null, false);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'PROJECT_SETTINGS')")
    public void exportSharedProject(long id, String description) {
        Project project = projectService.getProject(id);

        String fileName = "project_" + project.getUuid() + ".zip";

        byte[] projectData = createTemplate(id, description, true);

        FileEntry fileEntry =
            sharedTemplateFileStorage.storeFileContent(fileName, new ByteArrayInputStream(projectData));

        sharedTemplateService.save(UUID.fromString(Objects.requireNonNull(project.getUuid())), fileEntry);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public ProjectDTO getProject(long id) {
        Project project = projectService.getProject(id);

        return toProjectDTO(project);
    }

    /**
     * The domain-object read behind the GraphQL {@code project} query.
     *
     * <p>
     * Carries the same gate as {@link #getProject(long)} above, because it discloses the same project: the visibility
     * precondition plus the workspace scope, composed by {@code hasPermission(#id, 'Project', ...)}, so a
     * {@code PRIVATE} project withheld from the caller answers the same way here as it does through the DTO read.
     *
     * <p>
     * This method exists because {@code ProjectGraphQlController} used to run that check itself — a
     * {@code PermissionService.hasResourceScope} call in the method body and a hand-thrown
     * {@code AccessDeniedException} — for want of a facade method returning the entity its field resolvers need. An
     * in-body check is a check nothing audits and nothing inherits; this codebase keeps authorization on the facade.
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public Project getProjectRow(long id) {
        Project project = projectService.getProject(id);

        // A feature-owned system project answers as though it does not exist, which is what the listing already
        // implies by never showing one. Until this filter existed the pair disagreed: getProjectRows() dropped system
        // projects and this method returned them, so anyone holding the id of an agent's hidden __AI_AGENT__ project
        // could read it by id. Nothing secret leaked -- the name is __AI_AGENT__<uuid> and the description is empty --
        // but the javadoc pair claimed the two answered the same question, and they did not.
        //
        // NoSuchElementException rather than a denial, and thrown with no message, because it is exactly what
        // ProjectService.getProject raises for an absent id (OptionalUtils.get -> Optional.orElseThrow). A distinct
        // exception would let a caller tell "this id is a system project" from "this id is nothing", which is the
        // distinction being removed.
        if (SystemProjects.isSystemProject(project)) {
            throw new NoSuchElementException();
        }

        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectTemplateDTO getProjectTemplate(String id, boolean sharedProject) {
        byte[] data;

        if (sharedProject) {
            SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(id));

            if (sharedTemplate.getTemplate() == null) {
                throw new IllegalStateException("Shared template is not available");
            }

            try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                data = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to import shared project", e);
            }
        } else {
            data = preBuiltTemplateService.getPrebuiltTemplateData(id);
        }

        TemplateFiles templateFiles = readTemplate(data, true);

        ProjectInfo projectInfo = JsonUtils.read(templateFiles.projectJson, ProjectInfo.class);
        Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

        List<Workflow> workflows = getWorkflows(templateFiles);

        List<ComponentDefinitionTuple> componentDefinitions = getComponentDefinitions(workflows);

        ProjectTemplateDTO.ProjectInfo newProjectInfo = new ProjectTemplateDTO.ProjectInfo(
            projectInfo.name, projectInfo.description);
        List<WorkflowInfo> workflowInfos = workflows.stream()
            .map(workflow -> new WorkflowInfo(workflow.getId(), workflow.getLabel(), workflow.getDescription()))
            .toList();

        List<String> categories = template.categories == null || template.categories.isEmpty()
            ? List.of("other") : template.categories;

        return new ProjectTemplateDTO(
            template.authorName, template.authorEmail, template.authorRole, template.authorSocialLinks,
            categories, componentDefinitions, template.description, id, template.lastModifiedDate, newProjectInfo,
            template.projectVersion, publicUrl, workflowInfos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectTemplateDTO> getPreBuiltProjectTemplates(String query, String category) {
        return preBuiltTemplateService.getFiles("projects")
            .stream()
            .map(fileItem -> {
                try {
                    ProjectTemplateDTO projectTemplateDTO = getProjectTemplate(
                        EncodingUtils.base64EncodeToString(fileItem.path()), false);

                    if (StringUtils.isEmpty(query) && StringUtils.isEmpty(category)) {
                        return projectTemplateDTO;
                    } else {
                        if (StringUtils.isNotEmpty(query)) {
                            ProjectTemplateDTO.ProjectInfo projectInfo = projectTemplateDTO.project();

                            if (StringUtils.containsIgnoreCase(projectTemplateDTO.description(), query) ||
                                StringUtils.containsIgnoreCase(projectInfo.name(), query) ||
                                StringUtils.containsIgnoreCase(projectInfo.description(), query)) {

                                return projectTemplateDTO;
                            }
                        }

                        if (StringUtils.isNotEmpty(category)) {
                            List<String> categories = projectTemplateDTO.categories();

                            if (categories != null && categories.contains(category)) {
                                return projectTemplateDTO;
                            }
                        }

                        return null;
                    }
                } catch (Exception e) {
                    log.error("Failed to get workflow template", e);

                    return null;
                }

            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjects(Long categoryId, Boolean projectDeployments, Long tagId, Status status) {
        return getProjects(null, categoryId, tagId, projectDeployments, status, true, null);
    }

    /**
     * The listing behind the GraphQL {@code projects} query — every project the caller may open, and no more.
     *
     * <p>
     * The two halves below are the two halves {@code hasResourceScope(id, 'Project', scope)} composes for a project:
     * the workspace scope held in the project's OWN workspace, and the visibility precondition. Filtering on reach
     * alone left this listing tenant-wide — an EE member of one workspace could enumerate every WORKSPACE-visible
     * project in the tenant and then be denied opening any of them, which is the list-looser-than-by-id shape the
     * visibility model exists to close. There is no per-project environment resolver, so those two halves are the whole
     * of the by-id answer; {@code ProjectFacadeRowVisibilityTest} pins the agreement with {@link #getProjectRow(long)}.
     *
     * <p>
     * A project with no workspace cannot be scope-checked and is therefore not listed, rather than being listed
     * unchecked.
     *
     * <p>
     * The system-project filter is the one thing here with no counterpart on the by-id read, so it is the one place the
     * two answer differently — see {@link ProjectFacade#getProjectRow(long)} for what that discloses and why it is left
     * standing. The agreement test skips those ids for that reason.
     *
     * <p>
     * There is deliberately no {@code @PreAuthorize} here, and its absence is the honest description of this method: a
     * tenant-wide listing has no id to gate on, so the only expression an annotation could carry is
     * {@code isAuthenticated()} — and that is already the floor the body enforces. CE's
     * {@code PermissionService.hasWorkspaceScope} is literally {@code SecurityUtils.isAuthenticated()}, and EE's
     * returns false when there is no current user, so an unauthenticated caller is narrowed to nothing by the scope
     * check below rather than let through it. An annotation would add no protection and would change the answer from an
     * empty list to a denial; the surface audit records this method as filtered rather than guarded, which is what it
     * is.
     *
     * <p>
     * <strong>Cost, and why the two halves are batched differently.</strong> This reads every project row in the tenant
     * with no predicate, then makes one {@code hasWorkspaceScope} call per DISTINCT workspace, then one batched
     * visibility call. Only the last of those was made a single call, and {@code ProjectFacadeRowVisibilityTest} pins
     * it at {@code times(1)}. The scope half was deliberately left per-workspace. A
     * {@code hasWorkspaceScopes(Collection<Long>, scope)} on {@code PermissionService} was considered and not written:
     * CE's implementation is {@code isAuthenticated()}, so it has nothing to batch, and EE's would still resolve one
     * cached {@code WorkspaceScopeCacheService} entry per workspace and one {@code CustomRoleScopeResolver} lookup per
     * custom role behind it. The batch method would move the loop inside {@code PermissionService} rather than remove
     * it — and a {@code times(1)} test over it would then pin an API shape while asserting nothing about the work done,
     * which is the failure mode this whole body of tests exists to avoid. Adding it would also mean changing a
     * security-critical interface in CE and EE together for no measured gain.
     *
     * <p>
     * The loop is over distinct workspaces, which a tenant has few of, and each call is cache-served after the first;
     * the unbounded term here is the whole-tenant scan above it, which a scope batch would not touch. Not a regression
     * either way — {@code ProjectGraphQlController} did exactly this before the listing moved here. If this ever needs
     * fixing, the scan is the thing to fix: a repository predicate that returns only the rows of the workspaces the
     * caller is a member of.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Project> getProjectRows() {
        List<Project> projects = projectService.getProjects()
            .stream()
            .filter(project -> !SystemProjects.isSystemProject(project))
            .filter(project -> project.getWorkspaceId() != null)
            .toList();

        Set<Long> scopedWorkspaceIds = projects.stream()
            .map(Project::getWorkspaceId)
            .distinct()
            .filter(workspaceId -> permissionService.hasWorkspaceScope(workspaceId, "WORKFLOW_VIEW"))
            .collect(Collectors.toSet());

        return projectVisibilityFilter.filterVisible(
            projects.stream()
                .filter(project -> scopedWorkspaceIds.contains(project.getWorkspaceId()))
                .toList());
    }

    /**
     * The project's publication history.
     *
     * <p>
     * Carries the same gate as {@link #getProject(long)} beside it, because it discloses the same project: the version
     * list names each publication's description and date, so a caller who may not open the project may not read its
     * history either. The gate is the visibility precondition plus the workspace scope, composed by
     * {@code hasPermission(#id, 'Project', ...)}, so a {@code PRIVATE} project withheld from the caller answers the
     * same way here as it does through {@code getProject}.
     *
     * <p>
     * This method exists because {@code ProjectApiController} used to serve {@code GET
     * /internal/projects/{id}/versions} straight off {@code ProjectService}, reaching past the facade layer that owns
     * authorization — an open by-id read of any project's history, in any workspace, at any reach.
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public List<ProjectVersion> getProjectVersions(long id) {
        return projectService.getProjectVersions(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedProjectDTO getSharedProject(String projectUuid) {
        SharedProjectDTO sharedProjectDTO = null;
        Optional<SharedTemplate> sharedTemplateOptional = sharedTemplateService.fetchSharedTemplate(
            UUID.fromString(projectUuid));

        if (sharedTemplateOptional.isPresent()) {
            SharedTemplate sharedTemplate = sharedTemplateOptional.get();

            if (sharedTemplate.getTemplate() == null) {
                sharedProjectDTO = new SharedProjectDTO(false);
            } else {
                try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                    TemplateFiles templateFiles = readTemplate(inputStream.readAllBytes(), true);

                    Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

                    sharedProjectDTO = new SharedProjectDTO(
                        template.description, true, template.projectVersion, publicUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to import shared project", e);
                }
            }
        }

        return sharedProjectDTO;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, Long categoryId, boolean includeAllFields, Boolean projectDeployments, Status status,
        Long tagId, long workspaceId) {

        return getProjects(
            apiCollections, categoryId, tagId, projectDeployments, status, includeAllFields, workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public List<ProjectWorkflowDTO> getWorkspaceProjectWorkflows(long workspaceId) {
        return projectWorkflowService.getProjectWorkflows(getVisibleWorkspaceProjectIds(workspaceId))
            .stream()
            .map(projectWorkflow -> new ProjectWorkflowDTO(
                workflowService.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow, false))
            .toList();
    }

    /**
     * Returns every workspace project's latest-version workflows as one flat, label-only listing.
     *
     * <p>
     * Same set a caller would assemble by invoking {@code ProjectWorkflowFacade.getProjectWorkflows(projectId)} once
     * per project — latest project version only, sorted by label — but resolved in three batched queries (projects,
     * their project workflows, the workflows' labels) instead of one round trip per project. Unlike
     * {@link #getWorkspaceProjectWorkflows(long)}, which returns rows for ALL project versions and parses each workflow
     * definition, this one is safe to call for a workspace with hundreds of projects.
     * </p>
     */
    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    @Transactional(readOnly = true)
    public List<WorkspaceProjectWorkflowDTO> getWorkspaceLatestProjectWorkflows(long workspaceId) {
        List<Project> projects = projectVisibilityFilter.filterVisible(
            projectService.getProjects(null, null, false, null, null, workspaceId)
                .stream()
                .filter(project -> !SystemProjects.isSystemProject(project))
                .toList());

        if (projects.isEmpty()) {
            return List.of();
        }

        Map<Long, Project> projectMap = projects.stream()
            .collect(Collectors.toMap(project -> Objects.requireNonNull(project.getId(), "id"), Function.identity()));

        // One project workflow row exists per (project, version), so the batch load is filtered down to each
        // project's own last version. Doing the version filter here rather than per project keeps this to a single
        // repository call regardless of how many projects the workspace has.
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(List.copyOf(
            projectMap.keySet()))
            .stream()
            .filter(projectWorkflow -> {
                Project project = projectMap.get(projectWorkflow.getProjectId());

                return project != null && project.getLastProjectVersion() == projectWorkflow.getProjectVersion();
            })
            .toList();

        if (projectWorkflows.isEmpty()) {
            return List.of();
        }

        List<String> workflowIds = projectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .distinct()
            .toList();

        Map<String, Workflow> workflowMap = workflowService.getWorkflows(workflowIds)
            .stream()
            .collect(Collectors.toMap(Workflow::getId, Function.identity()));

        List<WorkspaceProjectWorkflowDTO> workspaceProjectWorkflows = new ArrayList<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            Workflow workflow = workflowMap.get(projectWorkflow.getWorkflowId());
            Project project = projectMap.get(projectWorkflow.getProjectId());

            // A project workflow row whose workflow blob is gone is skipped rather than surfaced as a nameless
            // entry — the same tolerance the chat-workflow listing applies.
            if (workflow == null || project == null) {
                continue;
            }

            workspaceProjectWorkflows.add(new WorkspaceProjectWorkflowDTO(
                projectWorkflow.getProjectId(), project.getName(),
                Objects.requireNonNull(projectWorkflow.getId(), "id"), workflow.getId(), workflow.getLabel()));
        }

        workspaceProjectWorkflows.sort(
            Comparator.comparing(WorkspaceProjectWorkflowDTO::projectName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(WorkspaceProjectWorkflowDTO::workflowLabel, String.CASE_INSENSITIVE_ORDER));

        return workspaceProjectWorkflows;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public long importProject(byte[] projectData, long workspaceId) {
        return importProjectTemplate(projectData, workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'PROJECT_CREATE')")
    public long importProjectTemplate(String id, long workspaceId, boolean sharedProject) {
        byte[] data;

        if (sharedProject) {
            SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(id));

            try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                data = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to import shared project", e);
            }
        } else {
            data = preBuiltTemplateService.getPrebuiltTemplateData(id);
        }

        return importProjectTemplate(data, workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Project', 'WORKFLOW_EDIT')")
    public int publishProject(long id, String description, boolean syncWithGit) {
        Project project = projectService.getProject(id);

        int oldProjectVersion = project.getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), oldProjectVersion);

        int newProjectVersion = projectService.publishProject(id, description, syncWithGit);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.publishWorkflow(
                project.getId(), oldProjectVersion, oldWorkflowId, oldProjectWorkflow);

            workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
        }

        return newProjectVersion;
    }

    @Override
    @PreAuthorize("hasPermission(#projectDTO.id, 'Project', 'WORKFLOW_EDIT')")
    public void updateProject(ProjectDTO projectDTO) {
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();
        Category category = projectDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        project.setTags(tags);

        projectService.update(project);
    }

    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')")
    public void updateProjectErrorWorkflow(long projectId, @Nullable Long errorProjectWorkflowId) {
        // Clearing needs no validation: there is no reference left to be invalid.
        if (errorProjectWorkflowId != null) {
            errorWorkflowConfigurationValidator.validate(projectId, errorProjectWorkflowId, null);
        }

        projectService.updateErrorWorkflow(projectId, errorProjectWorkflowId);
    }

    /**
     * CE force-writes WORKSPACE (there is no authorization boundary between members to withhold from); EE takes the
     * request, defaulting to the policy default and rejecting rungs the project type does not support.
     *
     * <p>
     * {@code defaultVisibility} throws for an unregistered resource type, which cannot happen here:
     * {@link com.bytechef.automation.configuration.security.ProjectVisibilityPolicy} is a {@code @Component} in this
     * very module, so every application carrying this facade also contributes the {@code "Project"} policy. No fallback
     * is written for that state on purpose — one would hide a genuine misconfiguration.
     */
    private void applyCreateVisibility(Project project, @Nullable ResourceVisibility requestedVisibility) {
        if (!eeEdition) {
            if (requestedVisibility != null && requestedVisibility != ResourceVisibility.WORKSPACE
                && log.isInfoEnabled()) {

                log.info(
                    "Forcing WORKSPACE visibility for project (requested={}, eeEdition=false)", requestedVisibility);
            }

            project.setVisibility(ResourceVisibility.WORKSPACE);

            return;
        }

        ResourceVisibility visibility = requestedVisibility == null
            ? resourceVisibilityPolicyRegistry.defaultVisibility(ProjectVisibilityFilter.PROJECT)
            : requestedVisibility;

        if (!resourceVisibilityPolicyRegistry.supports(ProjectVisibilityFilter.PROJECT, visibility)) {
            throw new ConfigurationException(
                "Project does not support %s visibility".formatted(visibility),
                ProjectErrorType.UNSUPPORTED_VISIBILITY);
        }

        project.setVisibility(visibility);
    }

    /**
     * Fail loud at startup when {@code bytechef.edition} is set to an unknown value so an accidental misconfiguration
     * (e.g. {@code Enterprise}, {@code ee }, a typo) does not silently disable EE features by falling through the
     * {@code "EE".equalsIgnoreCase(edition)} check.
     */
    private static void validateEdition(String edition) {
        if (edition == null || !("CE".equalsIgnoreCase(edition) || "EE".equalsIgnoreCase(edition))) {
            throw new IllegalStateException(
                "bytechef.edition must be CE or EE (case-insensitive); got '" + edition + "'");
        }
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private List<String> copyWorkflowIds(List<String> workflowIds) {
        List<String> newWorkflowIds = new ArrayList<>();

        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(
                workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());

            newWorkflowIds.add(workflow.getId());
        }
        return newWorkflowIds;
    }

    private byte[] createTemplate(long id, String description, boolean sharedTemplate) {
        Project project = projectService.getProject(id);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            id, project.getLastProjectVersion());

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            if (sharedTemplate) {
                ZipEntry zipEntry = new ZipEntry("template.json");

                zipOutputStream.putNextEntry(zipEntry);

                String templateJson = JsonUtils.write(new Template(description, project.getLastProjectVersion()));

                zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

                zipOutputStream.closeEntry();
            }

            ZipEntry projectZipEntry = new ZipEntry("project.json");

            zipOutputStream.putNextEntry(projectZipEntry);

            String projectJson = JsonUtils.write(new ProjectInfo(project));

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            Optional<CodeWorkflowSource> codeWorkflowSource = fetchCodeWorkflowSource(id);

            if (codeWorkflowSource.isPresent()) {
                CodeWorkflowSource source = codeWorkflowSource.get();

                ZipEntry codeWorkflowZipEntry = new ZipEntry(CODE_WORKFLOW_ENTRY_PREFIX + source.language());

                zipOutputStream.putNextEntry(codeWorkflowZipEntry);

                zipOutputStream.write(source.source()
                    .getBytes(StandardCharsets.UTF_8));

                zipOutputStream.closeEntry();
            }

            for (ProjectWorkflow projectWorkflow : projectWorkflows) {
                Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                ZipEntry workflowZipEntry = new ZipEntry(String.format("workflow-%s.json", projectWorkflow.getUuid()));

                zipOutputStream.putNextEntry(workflowZipEntry);

                String workflowJson = JsonUtils.write(workflow.getDefinition());

                zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export project", e);
        }
    }

    private String generateName(String oldName) {
        List<Project> projects = projectService.getProjects();
        int addendum = 0;

        for (Project curProject : projects) {
            String name = curProject.getName();

            if (name.startsWith(oldName)) {
                addendum++;
            }
        }

        return oldName + " (%s)".formatted(addendum);
    }

    private Category getCategory(Project project) {
        return project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId());
    }

    /**
     * Workspace project ids minus the feature-owned system projects (see {@link SystemProjects}) and minus the projects
     * the current principal may not see.
     */
    private List<Long> getVisibleWorkspaceProjectIds(long workspaceId) {
        List<Project> projects = projectService.getProjects(null, null, null, null, null, workspaceId)
            .stream()
            .filter(project -> !SystemProjects.isSystemProject(project))
            .toList();

        return projectVisibilityFilter.filterVisible(projects)
            .stream()
            .map(Project::getId)
            .toList();
    }

    private List<ProjectDTO> getProjects(
        Boolean apiCollections, Long categoryId, Long tagId, Boolean projectDeployments, Status status,
        boolean includeAllFields, Long workspaceId) {

        // Feature-owned system projects (Knowledge Base / Context Store sync workflows, embedded catalog projects)
        // are an implementation detail of those features, not something the user created — keep them out of the
        // projects listing. The visibility filter then drops the projects the current principal may not see, so this
        // listing agrees with the by-id gates rather than advertising rows they would refuse to open.
        List<Project> projects = projectVisibilityFilter.filterVisible(
            projectService.getProjects(apiCollections, categoryId, projectDeployments, tagId, status, workspaceId)
                .stream()
                .filter(project -> !SystemProjects.isSystemProject(project))
                .toList());

        if (includeAllFields) {
            List<Long> projectIds = projects.stream()
                .map(Project::getId)
                .toList();

            List<ProjectWorkflow> allProjectWorkflows = projectWorkflowService.getProjectWorkflows(projectIds);

            List<Category> categories = categoryService.getCategories(
                projects.stream()
                    .map(Project::getCategoryId)
                    .filter(Objects::nonNull)
                    .toList());

            List<Tag> allTags = tagService.getTags(
                projects.stream()
                    .flatMap(curProject -> CollectionUtils.stream(curProject.getTagIds()))
                    .filter(Objects::nonNull)
                    .toList());

            ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier =
                projectCodeWorkflowInfoSupplierProvider.getIfAvailable();

            Set<Long> codeWorkflowProjectIds = projectCodeWorkflowInfoSupplier == null
                ? Set.of()
                : Set.copyOf(projectCodeWorkflowInfoSupplier.getCodeWorkflowProjectIds());

            Map<Long, String> codeWorkflowLanguages = projectCodeWorkflowInfoSupplier == null
                ? Map.of()
                : projectCodeWorkflowInfoSupplier.getCodeWorkflowLanguages();

            return CollectionUtils.map(
                projects,
                project -> new ProjectDTO(
                    CollectionUtils.findFirstFilterOrElse(
                        categories,
                        category -> Objects.equals(project.getCategoryId(), category.getId()),
                        null),
                    project,
                    allProjectWorkflows.stream()
                        .filter(projectWorkflow -> Objects.equals(projectWorkflow.getProjectId(), project.getId()) &&
                            projectWorkflow.getProjectVersion() == project.getLastProjectVersion())
                        .map(ProjectWorkflow::getId)
                        .toList(),
                    CollectionUtils.filter(
                        allTags,
                        tag -> CollectionUtils.contains(project.getTagIds(), tag.getId())),
                    codeWorkflowProjectIds.contains(project.getId()),
                    codeWorkflowLanguages.get(project.getId())));
        } else {
            return CollectionUtils.map(projects, ProjectDTO::new);
        }
    }

    private List<Workflow> getWorkflows(TemplateFiles templateFiles) {
        List<Workflow> workflows = new ArrayList<>();

        for (String workflowJson : templateFiles.workflowJsons) {
            workflows.add(
                new Workflow(
                    String.valueOf(UUID.randomUUID()), JsonUtils.read(workflowJson, String.class),
                    Workflow.Format.JSON));
        }

        return workflows;
    }

    private List<ComponentDefinitionTuple> getComponentDefinitions(List<Workflow> workflows) {
        List<ComponentDefinitionTuple> componentDefinitions = new ArrayList<>();

        for (Workflow workflow : workflows) {
            String id = Objects.requireNonNull(workflow.getId());

            componentDefinitions.add(
                new ComponentDefinitionTuple(id, componentDefinitionHelper.getComponentDefinitions(workflow)));
        }

        return componentDefinitions;
    }

    private long importProjectTemplate(byte[] projectData, long workspaceId) {
        TemplateFiles templateFiles = readTemplate(projectData, false);

        ProjectInfo projectInfo = JsonUtils.read(templateFiles.projectJson, ProjectInfo.class);

        Project project = new Project();

        project.setName(projectInfo.name());
        project.setDescription(projectInfo.description());
        project.setWorkspaceId(workspaceId);

        ProjectDTO projectDTO = new ProjectDTO(project);

        long projectId = createProject(projectDTO);

        // A code-backed export carries its source rather than workflow definitions: deploying it regenerates the
        // workflows against this project's own code workflow container.
        if (templateFiles.codeWorkflowLanguage != null) {
            deployCodeWorkflowSource(projectId, templateFiles.codeWorkflowLanguage, templateFiles.codeWorkflowSource);

            return projectId;
        }

        for (String workflowJson : templateFiles.workflowJsons) {
            String definition = JsonUtils.read(workflowJson, String.class);

            projectWorkflowFacade.addWorkflow(projectId, definition);
        }

        return projectId;
    }

    private TemplateFiles readTemplate(byte[] data, boolean sharedTemplate) {
        String[] projectJson = {
            null
        };
        String[] templateJson = {
            null
        };
        String[] codeWorkflowLanguage = {
            null
        };
        String[] codeWorkflowSource = {
            null
        };
        List<String> workflowJsons = new ArrayList<>();

        try {
            BoundedZipReader.read(data, (name, entryData) -> {
                if ("project.json".equals(name)) {
                    projectJson[0] = new String(entryData, StandardCharsets.UTF_8);
                } else if ("template.json".equals(name)) {
                    templateJson[0] = new String(entryData, StandardCharsets.UTF_8);
                } else if (name.startsWith(CODE_WORKFLOW_ENTRY_PREFIX)) {
                    codeWorkflowLanguage[0] = name.substring(CODE_WORKFLOW_ENTRY_PREFIX.length());
                    codeWorkflowSource[0] = new String(entryData, StandardCharsets.UTF_8);
                } else if (name.startsWith("workflow-") && name.endsWith(".json")) {
                    workflowJsons.add(new String(entryData, StandardCharsets.UTF_8));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shared project", e);
        }

        // A code-backed export needs no workflow definitions — its source generates them on import.
        if (projectJson[0] == null || sharedTemplate && (templateJson[0] == null)
            || workflowJsons.isEmpty() && codeWorkflowSource[0] == null) {

            throw new RuntimeException("Missing files in a shared project file");
        }

        return new TemplateFiles(
            templateJson[0], projectJson[0], workflowJsons, codeWorkflowLanguage[0], codeWorkflowSource[0]);
    }

    private Optional<CodeWorkflowSource> fetchCodeWorkflowSource(long projectId) {
        ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier =
            projectCodeWorkflowInfoSupplierProvider.getIfAvailable();

        return projectCodeWorkflowInfoSupplier == null
            ? Optional.empty()
            : projectCodeWorkflowInfoSupplier.fetchCodeWorkflowSource(projectId);
    }

    private void deployCodeWorkflowSource(long projectId, String language, String source) {
        ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier =
            projectCodeWorkflowInfoSupplierProvider.getIfAvailable();

        if (projectCodeWorkflowInfoSupplier == null) {
            throw new IllegalStateException("Code workflows are not available in this edition");
        }

        projectCodeWorkflowInfoSupplier.deployCodeWorkflowSource(projectId, language, source);
    }

    private ProjectDTO toProjectDTO(Project project) {
        ProjectCodeWorkflowInfoSupplier projectCodeWorkflowInfoSupplier =
            projectCodeWorkflowInfoSupplierProvider.getIfAvailable();

        Optional<CodeWorkflowInfo> codeWorkflowInfo = projectCodeWorkflowInfoSupplier == null
            ? Optional.empty()
            : projectCodeWorkflowInfoSupplier.fetchCodeWorkflowInfo(project.getId());

        return new ProjectDTO(
            getCategory(project), project,
            projectWorkflowService.getProjectProjectWorkflowIds(project.getId(), project.getLastProjectVersion()),
            tagService.getTags(project.getTagIds()), codeWorkflowInfo.isPresent(),
            codeWorkflowInfo.map(CodeWorkflowInfo::language)
                .orElse(null));
    }

    record ProjectInfo(String name, String description) {

        ProjectInfo(Project project) {
            this(project.getName(), project.getDescription());
        }
    }

    private record Template(
        String authorName, String authorRole, String authorEmail, String authorSocialLinks, List<String> categories,
        String description, Instant lastModifiedDate, Integer projectVersion) {

        public Template(String description, int lastProjectVersion) {
            this(null, null, null, null, Collections.emptyList(), description, Instant.now(), lastProjectVersion);
        }
    }

    private record TemplateFiles(
        String templateJson, String projectJson, List<String> workflowJsons, String codeWorkflowLanguage,
        String codeWorkflowSource) {
    }
}
