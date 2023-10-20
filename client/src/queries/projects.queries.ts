import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    ProjectCategoriesApi,
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
    projectWorkflows: (id: number) => ['projectWorkflows', id],
    projects: ['projects'] as const,
};

export const useGetProjectCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(
        ProjectKeys.projectCategories,
        () => new ProjectCategoriesApi().getProjectCategories(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetProjectTagsQuery = () =>
    useQuery<TagModel[], Error>(
        ProjectKeys.projectTags,
        () => new ProjectTagsApi().getProjectTags(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetProjectQuery = (id: number, initialData?: ProjectModel) =>
    useQuery<ProjectModel, Error>(
        ProjectKeys.project(id),
        () => new ProjectsApi().getProject({id}),
        {
            staleTime: 1 * 60 * 1000,
            initialData,
        }
    );

export const useGetProjectsQuery = (filters: {
    categoryIds?: number[];
    tagIds?: number[];
}) =>
    useQuery<ProjectModel[], Error>(
        ProjectKeys.projectList(filters),
        () => new ProjectsApi().getProjects(filters),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(
        ProjectKeys.projectWorkflows(id),
        () => new ProjectsApi().getProjectWorkflows({id}),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
