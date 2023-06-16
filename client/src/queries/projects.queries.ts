import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    PageModel,
    ProjectCategoriesApi,
    ProjectInstanceModel,
    ProjectInstanceTagsApi,
    ProjectInstancesApi,
    ProjectModel,
    ProjectTagsApi,
    ProjectsApi,
    TagModel,
} from 'middleware/automation/configuration';

import {
    GetProjectWorkflowExecutionRequest,
    GetProjectWorkflowExecutionsRequest,
    ProjectWorkflowExecutionModel,
    ProjectWorkflowExecutionsApi,
} from '../middleware/automation/execution';
import {WorkflowModel} from '../middleware/core/workflow/configuration';

export const ProjectKeys = {
    project: (id: number) => ['project', id],
    projectCategories: ['projectCategories'] as const,
    projectInstanceList: (filters: {
        projectIds?: number[];
        tagIds?: number[];
    }) => [...ProjectKeys.projectInstances, filters],
    projectInstanceTags: ['projectInstanceTags'] as const,
    projectInstances: ['projectInstances'] as const,
    projectList: (
        filters: {categoryIds?: number[]; tagIds?: number[]} | undefined
    ) => [...ProjectKeys.projects, filters],
    projectTags: ['projectTags'] as const,
    projectWorkflows: (id: number) => [
        ...ProjectKeys.projects,
        id,
        'projectWorkflows',
    ],
    projects: ['projects'] as const,
    workflowExecution: (request: GetProjectWorkflowExecutionRequest) => [
        'workflowExecution',
        request,
    ],
    workflowExecutions: (filter: GetProjectWorkflowExecutionsRequest) => [
        'workflowExecutions',
        filter,
    ],
};

export const useGetProjectCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(ProjectKeys.projectCategories, () =>
        new ProjectCategoriesApi().getProjectCategories()
    );

export const useGetProjectInstancesQuery = (filters: {
    projectIds?: number[];
    tagIds?: number[];
}) =>
    useQuery<ProjectInstanceModel[], Error>(
        ProjectKeys.projectInstanceList(filters),
        () => new ProjectInstancesApi().getProjectInstances(filters)
    );

export const useGetProjectInstanceTagsQuery = () =>
    useQuery<TagModel[], Error>(ProjectKeys.projectInstanceTags, () =>
        new ProjectInstanceTagsApi().getProjectInstanceTags()
    );

export const useGetProjectTagsQuery = () =>
    useQuery<TagModel[], Error>(ProjectKeys.projectTags, () =>
        new ProjectTagsApi().getProjectTags()
    );

export const useGetProjectQuery = (id: number, initialData?: ProjectModel) =>
    useQuery<ProjectModel, Error>(
        ProjectKeys.project(id),
        () => new ProjectsApi().getProject({id}),
        {
            initialData,
        }
    );

export const useGetProjectsQuery = (filters?: {
    categoryIds?: number[];
    projectInstances?: boolean;
    tagIds?: number[];
}) =>
    useQuery<ProjectModel[], Error>(ProjectKeys.projectList(filters), () =>
        new ProjectsApi().getProjects(filters)
    );

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(ProjectKeys.projectWorkflows(id), () =>
        new ProjectsApi().getProjectWorkflows({id})
    );

export const useGetWorkflowExecutionsQuery = (
    request: GetProjectWorkflowExecutionsRequest
) =>
    useQuery<PageModel, Error>(ProjectKeys.workflowExecutions(request), () =>
        new ProjectWorkflowExecutionsApi().getProjectWorkflowExecutions(request)
    );

export const useGetWorkflowExecutionQuery = (
    request: GetProjectWorkflowExecutionRequest,
    isEnabled: boolean
) =>
    useQuery<ProjectWorkflowExecutionModel, Error>(
        ProjectKeys.workflowExecution(request),
        () =>
            new ProjectWorkflowExecutionsApi().getProjectWorkflowExecution(
                request
            ),
        {
            enabled: isEnabled,
        }
    );
