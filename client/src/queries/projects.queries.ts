import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    GetProjectExecutionsRequest,
    PageModel,
    ProjectCategoriesApi,
    ProjectExecutionsApi,
    ProjectInstanceModel,
    ProjectInstanceTagsApi,
    ProjectInstancesApi,
    ProjectModel,
    ProjectTagsApi,
    ProjectsApi,
    TagModel,
} from 'middleware/project';

import {WorkflowModel} from '../middleware/workflow';

export const ProjectKeys = {
    project: (id: number) => ['project', id],
    projectCategories: ['projectCategories'] as const,
    projectInstanceTags: ['projectInstanceTags'] as const,
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
    projectInstanceList: (filters: {
        projectIds?: number[];
        tagIds?: number[];
    }) => [...ProjectKeys.projectInstances, filters],
    projectInstances: ['projectInstances'] as const,
    projects: ['projects'] as const,
    projectExecutions: (filter: GetProjectExecutionsRequest) => [
        'projectExecutions',
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

export const useGetProjectsQuery = (filters: {
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

export const useGetProjectExecutionsQuery = (
    request: GetProjectExecutionsRequest
) =>
    useQuery<PageModel, Error>(ProjectKeys.projectExecutions(request), () =>
        new ProjectExecutionsApi().getProjectExecutions(request)
    );
