import {
    GetExecutionRequest,
    GetExecutionsRequest,
    WorkflowExecutionApi,
    WorkflowExecutionModel,
} from '@/middleware/helios/execution';
import {useQuery} from '@tanstack/react-query';
import {
    CategoryApi,
    CategoryModel,
    PageModel,
    ProjectApi,
    ProjectInstanceApi,
    ProjectInstanceModel,
    ProjectInstanceTagApi,
    ProjectModel,
    ProjectTagApi,
    TagModel,
    WorkflowApi,
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
    workflowExecution: (request: GetExecutionRequest) => [
        'workflowExecution',
        request,
    ],
    workflowExecutions: (filter: GetExecutionsRequest) => [
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
        new CategoryApi().getProjectCategories()
    );

export const useGetProjectInstancesQuery = (filters: {
    projectId?: number;
    tagId?: number;
}) =>
    useQuery<ProjectInstanceModel[], Error>(
        ProjectKeys.projectInstanceList(filters),
        () => new ProjectInstanceApi().getProjectInstances(filters)
    );

export const useGetProjectInstanceTagsQuery = () =>
    useQuery<TagModel[], Error>(ProjectKeys.projectInstanceTags, () =>
        new ProjectInstanceTagApi().getProjectInstanceTags()
    );

export const useGetProjectTagsQuery = () =>
    useQuery<TagModel[], Error>(ProjectKeys.projectTags, () =>
        new ProjectTagApi().getProjectTags()
    );

export const useGetProjectQuery = (id: number, initialData?: ProjectModel) =>
    useQuery<ProjectModel, Error>(
        ProjectKeys.project(id),
        () => new ProjectApi().getProject({id}),
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
        new ProjectApi().getProjects(filters)
    );

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(ProjectKeys.projectWorkflows(id), () =>
        new WorkflowApi().getProjectWorkflows({id})
    );

export const useGetExecutionsQuery = (request: GetExecutionsRequest) =>
    useQuery<PageModel, Error>(ProjectKeys.workflowExecutions(request), () =>
        new WorkflowExecutionApi().getExecutions(request)
    );

export const useGetWorkflowExecutionQuery = (
    request: GetExecutionRequest,
    isEnabled: boolean
) =>
    useQuery<WorkflowExecutionModel, Error>(
        ProjectKeys.workflowExecution(request),
        () => new WorkflowExecutionApi().getExecution(request),
        {
            enabled: isEnabled,
        }
    );

export const useGetWorkflowsQuery = () =>
    useQuery<WorkflowModel[], Error>(WorkflowKeys.workflows, () =>
        new WorkflowApi().getWorkflows()
    );
