import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    GetProjectExecutionsRequest,
    PageModel,
    ProjectCategoriesApi,
    ProjectExecutionsApi,
    ProjectModel,
    ProjectsApi,
    ProjectTagsApi,
    TagModel,
} from 'middleware/project';
import {WorkflowModel} from '../middleware/workflow';

export const ProjectKeys = {
    project: (id: number) => ['project', id],
    projectCategories: ['projectCategories'] as const,
    projectList: (filters: {categoryIds?: number[]; tagIds?: number[]}) => [
        ...ProjectKeys.projects,
        filters,
    ],
    projectTags: ['projectTags'] as const,
    projectWorkflows: (id: number) => [
        ...ProjectKeys.projects,
        id,
        'projectWorkflows',
    ],
    projects: ['projects'] as const,
    projectExecutions: (filter: GetProjectExecutionsRequest) => [
        'projectExecutions',
        filter,
    ],
    projectInstances: ['projectInstances'] as const,
};

export const useGetProjectCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(ProjectKeys.projectCategories, () =>
        new ProjectCategoriesApi().getProjectCategories()
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

export const useGetProjectsQuery = (filters: {
    categoryIds?: number[];
    tagIds?: number[];
}) =>
    useQuery<ProjectModel[], Error>(ProjectKeys.projectList(filters), () =>
        new ProjectsApi().getProjects(filters)
    );

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(ProjectKeys.projectWorkflows(id), () =>
        new ProjectsApi().getProjectWorkflows({id})
    );

export const useGetProjectExecutionsQuery = (
    request: GetProjectExecutionsRequest
) =>
    useQuery<PageModel, Error>(ProjectKeys.projectExecutions(request), () =>
        new ProjectExecutionsApi().getProjectExecutions(request)
    );
