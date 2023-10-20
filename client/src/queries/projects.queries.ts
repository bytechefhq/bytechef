import {
    GetProjectWorkflowExecutionRequest,
    GetProjectWorkflowExecutionsRequest,
    ProjectWorkflowExecutionModel,
    ProjectWorkflowExecutionsApi,
} from '@/middleware/helios/execution';
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
    ProjectWorkflowsApi,
    ProjectsApi,
    TagModel,
    WorkflowModel,
} from 'middleware/helios/configuration';

export const ProjectKeys = {
    project: (id: number) => ['project', id],
    projectCategories: ['projectCategories'] as const,
    projectInstanceList: (filters: {projectId?: number; tagId?: number}) => [
        ...ProjectKeys.projectInstances,
        filters,
    ],
    projectInstanceTags: ['projectInstanceTags'] as const,
    projectInstances: ['projectInstances'] as const,
    projectList: (
        filters: {categoryId?: number; tagId?: number} | undefined
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

export const WorkflowKeys = {
    workflow: (id: number) => ['workflow', id],
    workflows: ['workflows'] as const,
};

export const useGetProjectCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(ProjectKeys.projectCategories, () =>
        new ProjectCategoriesApi().getProjectCategories()
    );

export const useGetProjectInstancesQuery = (filters: {
    projectId?: number;
    tagId?: number;
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
    categoryId?: number;
    projectInstances?: boolean;
    tagId?: number;
}) =>
    useQuery<ProjectModel[], Error>(ProjectKeys.projectList(filters), () =>
        new ProjectsApi().getProjects(filters)
    );

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(ProjectKeys.projectWorkflows(id), () =>
        new ProjectWorkflowsApi().getProjectWorkflows({id})
    );

export const useGetProjectWorkflowExecutionsQuery = (
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

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>(WorkflowKeys.workflows, () =>
        new ProjectWorkflowsApi().getWorkflows()
    );
