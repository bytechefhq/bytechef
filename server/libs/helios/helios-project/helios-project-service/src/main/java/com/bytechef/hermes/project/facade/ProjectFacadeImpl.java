
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.project.facade;

import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.project.domain.Project;
import com.bytechef.category.service.CategoryService;
import com.bytechef.hermes.project.domain.ProjectInstance;
import com.bytechef.hermes.project.dto.ProjectExecutionDTO;
import com.bytechef.hermes.project.service.ProjectInstanceService;
import com.bytechef.hermes.project.service.ProjectService;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ProjectFacadeImpl implements ProjectFacade {

    private final CategoryService categoryService;
    private final JobService jobService;
    private final ProjectService projectService;
    private final ProjectInstanceService projectInstanceService;
    private final TagService tagService;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, JobService jobService, ProjectService projectService,
        ProjectInstanceService projectInstanceService, TagService tagService, TaskExecutionService taskExecutionService,
        WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.jobService = jobService;
        this.projectService = projectService;
        this.projectInstanceService = projectInstanceService;
        this.tagService = tagService;
        this.taskExecutionService = taskExecutionService;
        this.workflowService = workflowService;
    }

    @Override
    public Project addWorkflow(long id, String name, String description, String definition) {
        if (definition == null) {
            definition = "{\"label\": \"%s\", \"description\": \"%s\", \"tasks\": []}"
                .formatted(name, description);
        }

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        return projectService.addWorkflow(id, workflow.getId());
    }

    @Override
    @SuppressFBWarnings("NP")
    public Project createProject(Project project) {
        if (project.getCategory() != null) {
            Category category = project.getCategory();

            project.setCategory(categoryService.save(category));
        }

        if (org.springframework.util.CollectionUtils.isEmpty(project.getWorkflowIds())) {
            Workflow workflow = workflowService.create(null, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            project.setWorkflowIds(List.of(workflow.getId()));
        }

        if (!org.springframework.util.CollectionUtils.isEmpty(project.getTags())) {
            project.setTags(tagService.save(project.getTags()));
        }

        return projectService.create(project);
    }

    @Override
    public ProjectInstance createProjectInstance(ProjectInstance projectInstance) {
        if (!org.springframework.util.CollectionUtils.isEmpty(projectInstance.getTags())) {
            projectInstance.setTags(tagService.save(projectInstance.getTags()));
        }

        return projectInstanceService.create(projectInstance);
    }

    @Override
    public void deleteProject(Long projectId) {
        Project project = projectService.getProject(projectId);

        for (String workflowId : project.getWorkflowIds()) {
            workflowService.delete(workflowId);
        }

        projectService.delete(projectId);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteProjectInstance(Long projectInstanceId) {
        projectService.delete(projectInstanceId);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public Project duplicateProject(long projectId) {
        Project project = getProject(projectId);

        List<String> workflowIds = new ArrayList<>();

        for (String workflowId : project.getWorkflowIds()) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());

            workflowIds.add(workflow.getId());
        }

        List<Project> projects = projectService.getProjects();

        int addendum = 0;

        for (Project curProject : projects) {
            String name = curProject.getName();

            if (name.startsWith(project.getName())) {
                addendum++;
            }
        }

        project.setId(null);
        project.setName(project.getName() + " (%s)".formatted(addendum));
        project.setVersion(0);
        project.setWorkflowIds(workflowIds);

        return projectService.create(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Project getProject(Long projectId) {
        Project project = projectService.getProject(projectId);

        if (project.getCategoryId() != null) {
            OptionalUtils.ifPresent(categoryService.fetchCategory(project.getCategoryId()), project::setCategory);
        }

        project.setTags(tagService.getTags(project.getTagIds()));

        return project;
    }

    @Override
    public ProjectInstance getProjectInstance(Long projectInstanceId) {
        ProjectInstance projectInstance = projectInstanceService.getProjectInstance(projectInstanceId);

        projectInstance.setProject(projectService.getProject(projectInstance.getProjectId()));
        projectInstance.setTags(tagService.getTags(projectInstance.getTagIds()));

        return projectInstance;
    }

    @Override
    public List<Category> getProjectCategories() {
        List<Project> projects = projectService.getProjects();

        List<Long> categoryIds = projects.stream()
            .map(Project::getCategoryId)
            .filter(Objects::nonNull)
            .toList();

        return categoryService.getCategories(categoryIds);
    }

    @Override
    public List<Tag> getProjectInstanceTags() {
        List<ProjectInstance> projectInstances = projectInstanceService.getProjectInstances();

        List<Long> tagIds = projectInstances.stream()
            .map(ProjectInstance::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getProjectTags() {
        List<Project> projects = projectService.getProjects();

        List<Long> tagIds = projects.stream()
            .map(Project::getTagIds)
            .flatMap(Collection::stream)
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    public List<Workflow> getProjectWorkflows(Long id) {
        Project project = projectService.getProject(id);

        return workflowService.getWorkflows(project.getWorkflowIds());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProjectExecutionDTO> searchProjectExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber) {

        List<ProjectInstance> projectInstances;

        if (projectInstanceId == null) {
            projectInstances = projectInstanceService.getProjectInstances();
        } else {
            projectInstances = List.of(projectInstanceService.getProjectInstance(projectInstanceId));
        }

        List<Project> projects;

        if (projectId == null) {
            projects = projectService.getProjects();
        } else {
            projects = List.of(projectService.getProject(projectId));
        }

        List<String> projectWorkflowIds = Collections.emptyList();

        if (projectId != null) {
            Project project = projects.get(0);

            projectWorkflowIds = project.getWorkflowIds();
        }

        Page<Job> jobsPage = jobService.searchJobs(
            jobStatus, jobStartDate, jobEndDate, workflowId, projectWorkflowIds, pageNumber);

        List<TaskExecution> taskExecutions = taskExecutionService.getJobsTaskExecutions(
            CollectionUtils.map(jobsPage.toList(), Job::getId));

        List<Workflow> workflows = workflowService.getWorkflows(
            CollectionUtils.map(jobsPage.toList(), Job::getWorkflowId));

        return jobsPage.map(job -> new ProjectExecutionDTO(
            null,
            job,
            CollectionUtils.findFirst(
                projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId())),
            CollectionUtils.filter(
                taskExecutions, taskExecution -> Objects.equals(taskExecution.getJobId(), job.getId())),
            CollectionUtils.findFirst(
                workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId()))));
    }

    @Override
    public List<ProjectInstance> searchProjectInstances(List<Long> projectIds, List<Long> tagIds) {
        List<ProjectInstance> projectInstances = projectInstanceService.searchProjectInstances(projectIds, tagIds);

        List<Project> projects = projectService.getProjects(
            projectInstances.stream()
                .map(project -> project.getProjectId())
                .filter(Objects::nonNull)
                .toList());

        List<Tag> tags = tagService.getTags(
            projectInstances.stream()
                .flatMap(project -> CollectionUtils.stream(project.getTagIds()))
                .filter(Objects::nonNull)
                .toList());

        for (ProjectInstance projectInstance : projectInstances) {
            projectInstance.setProject(
                CollectionUtils.findFirst(
                    projects, project -> Objects.equals(project.getId(), projectInstance.getProjectId())));
            projectInstance.setTags(
                CollectionUtils.filter(
                    tags,
                    tag -> {
                        List<Long> curTagIds = projectInstance.getTagIds();

                        return curTagIds.contains(tag.getId());
                    }));
        }

        return projectInstances;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> searchProjects(List<Long> categoryIds, boolean projectInstances, List<Long> tagIds) {
        List<Long> projectIds = null;

        if (projectInstances) {
            projectIds = projectInstanceService.getProjectIds();
        }

        List<Project> projects = projectService.searchProjects(categoryIds, projectIds, tagIds);

        List<Category> categories = categoryService.getCategories(projects.stream()
            .map(Project::getCategoryId)
            .filter(Objects::nonNull)
            .toList());

        for (Category category : categories) {
            projects.stream()
                .filter(project -> Objects.equals(project.getCategoryId(), category.getId()))
                .forEach(project -> project.setCategory(category));
        }

        List<Tag> tags = tagService.getTags(
            projects.stream()
                .flatMap(project -> CollectionUtils.stream(project.getTagIds()))
                .filter(Objects::nonNull)
                .toList());

        for (Project project : projects) {
            project.setTags(
                CollectionUtils.filter(
                    tags,
                    tag -> {
                        List<Long> curTagIds = project.getTagIds();

                        return curTagIds.contains(tag.getId());
                    }));
        }

        return projects;
    }

    @Override
    public Project update(Project project) {
        project
            .setCategory(project.getCategory() == null ? null : categoryService.save(project.getCategory()));
        project
            .setTags(
                org.springframework.util.CollectionUtils.isEmpty(project.getTags())
                    ? Collections.emptyList()
                    : tagService.save(project.getTags()));

        return projectService.update(project);
    }

    @Override
    public ProjectInstance update(ProjectInstance projectInstance) {
        projectInstance
            .setTags(
                org.springframework.util.CollectionUtils.isEmpty(projectInstance.getTags())
                    ? Collections.emptyList()
                    : tagService.save(projectInstance.getTags()));

        return projectInstanceService.update(projectInstance);
    }

    @Override
    public ProjectInstance updateProjectInstanceTags(Long projectInstanceId, List<Tag> tags) {
        tags = org.springframework.util.CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        return projectInstanceService.update(projectInstanceId, tags);
    }

    @Override
    public Project updateProjectTags(Long projectId, List<Tag> tags) {
        tags = org.springframework.util.CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        return projectService.update(projectId, tags);
    }
}
