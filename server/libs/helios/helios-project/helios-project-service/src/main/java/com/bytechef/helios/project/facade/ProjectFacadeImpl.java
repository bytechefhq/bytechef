
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

package com.bytechef.helios.project.facade;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.service.ContextService;
import com.bytechef.atlas.service.JobService;
import com.bytechef.atlas.service.TaskExecutionService;
import com.bytechef.atlas.service.WorkflowService;
import com.bytechef.category.domain.Category;
import com.bytechef.category.service.CategoryService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.dto.ProjectDTO;
import com.bytechef.helios.project.dto.ProjectExecutionDTO;
import com.bytechef.atlas.dto.TaskExecutionDTO;
import com.bytechef.helios.project.service.ProjectInstanceService;
import com.bytechef.helios.project.service.ProjectService;
import com.bytechef.hermes.connection.WorkflowConnection;
import com.bytechef.hermes.workflow.WorkflowDTO;
import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Page;
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
@Transactional
public class ProjectFacadeImpl implements ProjectFacade {

    private final CategoryService categoryService;
    private final ContextService contextService;
    private final JobService jobService;
    private final ProjectService projectService;
    private final ProjectInstanceService projectInstanceService;
    private final TagService tagService;
    private final TaskExecutionService taskExecutionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        CategoryService categoryService, ContextService contextService, JobService jobService,
        ProjectInstanceService projectInstanceService, ProjectService projectService,
        TaskExecutionService taskExecutionService, TagService tagService, WorkflowService workflowService) {

        this.categoryService = categoryService;
        this.contextService = contextService;
        this.jobService = jobService;
        this.projectInstanceService = projectInstanceService;
        this.projectService = projectService;
        this.taskExecutionService = taskExecutionService;
        this.tagService = tagService;
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowDTO addWorkflow(long id, String label, String description, String definition) {
        if (definition == null) {
            definition = "{\"description\": \"%s\", \"label\": \"%s\", \"tasks\": []}"
                .formatted(description, label);
        }

        Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        projectService.addWorkflow(id, workflow.getId());

        return new WorkflowDTO(WorkflowConnection.of(workflow), workflow);
    }

    @Override
    @SuppressFBWarnings("NP")
    public ProjectDTO createProject(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();

        Category category = projectDTO.category();

        if (projectDTO.category() != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        if (org.springframework.util.CollectionUtils.isEmpty(projectDTO.workflowIds())) {
            Workflow workflow = workflowService.create(null, Workflow.Format.JSON, Workflow.SourceType.JDBC);

            project.setWorkflowIds(List.of(workflow.getId()));
        }

        List<Tag> tags = projectDTO.tags();

        if (!org.springframework.util.CollectionUtils.isEmpty(tags)) {
            tags = tagService.save(tags);

            project.setTags(tags);
        }

        return new ProjectDTO(projectService.create(project), category, tags);
    }

    @Override
    public void deleteProject(long id) {
        Project project = projectService.getProject(id);

        for (String workflowId : project.getWorkflowIds()) {
            workflowService.delete(workflowId);
        }

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        project.setId(null);
        project.setName(generateName(project.getName()));
        project.setVersion(0);
        project.setWorkflowIds(copyWorkflowIds(project.getWorkflowIds()));

        project = projectService.create(project);

        return new ProjectDTO(
            project,
            project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tagService.getTags(project.getTagIds()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO getProject(long id) {
        Project project = projectService.getProject(id);

        return new ProjectDTO(
            project,
            project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tagService.getTags(project.getTagIds()));
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
    @Transactional(readOnly = true)
    public List<WorkflowDTO> getProjectWorkflows(long id) {
        Project project = projectService.getProject(id);

        return CollectionUtils.map(
            workflowService.getWorkflows(project.getWorkflowIds()),
            workflow -> new WorkflowDTO(WorkflowConnection.of(workflow), workflow));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public ProjectExecutionDTO getProjectExecution(long id) {
        Job job = jobService.getJob(id);

        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(job.getId());

        return new ProjectExecutionDTO(
            job.getId(),
            OptionalUtils.orElse(projectInstanceService.fetchJobProjectInstance(job.getId()), null),
            job,
            OptionalUtils.orElse(projectService.fetchJobProject(id), null),
            CollectionUtils.map(taskExecutions, taskExecution -> new TaskExecutionDTO(
                contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION), taskExecution)),
            workflowService.getWorkflow(job.getWorkflowId()));
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressFBWarnings("NP")
    public Page<ProjectExecutionDTO> searchProjectExecutions(
        String jobStatus, LocalDateTime jobStartDate, LocalDateTime jobEndDate, Long projectId, Long projectInstanceId,
        String workflowId, Integer pageNumber) {

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
            job.getId(),
            OptionalUtils.orElse(projectInstanceService.fetchJobProjectInstance(job.getId()), null),
            job,
            CollectionUtils.getFirst(
                projects, project -> CollectionUtils.contains(project.getWorkflowIds(), job.getWorkflowId())),
            CollectionUtils.filter(
                taskExecutions,
                taskExecution -> Objects.equals(taskExecution.getJobId(), job.getId()),
                taskExecution -> new TaskExecutionDTO(
                    contextService.peek(taskExecution.getId(), Context.Classname.TASK_EXECUTION), taskExecution)),
            CollectionUtils.getFirst(
                workflows, workflow -> Objects.equals(workflow.getId(), job.getWorkflowId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> searchProjects(List<Long> categoryIds, boolean projectInstances, List<Long> tagIds) {
        List<Long> projectIds = null;

        if (projectInstances) {
            projectIds = projectInstanceService.getProjectIds();
        }

        List<Project> projects = projectService.searchProjects(categoryIds, projectIds, tagIds);

        return CollectionUtils.map(
            projects,
            project -> new ProjectDTO(
                project,
                CollectionUtils.findFirstOrElse(
                    categoryService.getCategories(
                        CollectionUtils.map(projects, Project::getCategoryId, Objects::nonNull)),
                    category -> Objects.equals(project.getCategoryId(), category.getId()),
                    null),
                CollectionUtils.filter(
                    tagService.getTags(
                        CollectionUtils.flatMap(
                            projects, curProject -> CollectionUtils.stream(
                                curProject.getTagIds()),
                            Objects::nonNull)),
                    tag -> CollectionUtils.contains(project.getTagIds(), tag.getId()))));
    }

    @Override
    public ProjectDTO update(ProjectDTO projectDTO) {
        Category category = projectDTO.category() == null ? null : categoryService.save(projectDTO.category());
        List<Tag> tags = org.springframework.util.CollectionUtils.isEmpty(projectDTO.tags())
            ? Collections.emptyList()
            : tagService.save(projectDTO.tags());

        return new ProjectDTO(
            projectService.update(
                projectDTO.id(), category == null ? null : category.getId(), projectDTO.description(),
                projectDTO.name(), CollectionUtils.map(tags, Tag::getId), projectDTO.workflowIds()),
            category, tags);
    }

    @Override
    public void updateProjectTags(long id, List<Tag> tags) {
        tags = org.springframework.util.CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);

        Project project = projectService.update(id, CollectionUtils.map(tags, Tag::getId));

        new ProjectDTO(
            project,
            project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId()),
            tags);
    }

    private List<String> copyWorkflowIds(List<String> workflowIds) {
        List<String> newWorkflowIds = new ArrayList<>();

        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());

            newWorkflowIds.add(workflow.getId());
        }
        return newWorkflowIds;
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
}
