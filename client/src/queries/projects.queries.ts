import {useQuery} from '@tanstack/react-query';
import {
    CategoryApi,
    CategoryModel,
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
    filteredProjectInstances: (filters: {
        projectId?: number;
        tagId?: number;
    }) => [...ProjectKeys.projectInstances, filters],
    filteredProjects: (
        filters: {categoryId?: number; tagId?: number} | undefined
    ) => [...ProjectKeys.projects, filters],
    project: (id: number) => [...ProjectKeys.projects, id],
    projectCategories: ['projectCategories'] as const,
    projectInstanceTags: ['projectInstanceTags'] as const,
    projectInstances: ['projectInstances'] as const,
    projectTags: ['projectTags'] as const,
    projectWorkflows: (id: number) => [
        ...ProjectKeys.projects,
        id,
        'projectWorkflows',
    ],
    projects: ['projects'] as const,
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
        ProjectKeys.filteredProjectInstances(filters),
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
    published?: boolean;
}) =>
    useQuery<ProjectModel[], Error>(ProjectKeys.filteredProjects(filters), () =>
        new ProjectApi().getProjects(filters)
    );

export const useGetProjectWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(ProjectKeys.projectWorkflows(id), () =>
        new WorkflowApi().getProjectWorkflows({id})
    );
