/* eslint-disable sort-keys */
import {Category, CategoryApi} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectCategoryKeys = {
    projectCategories: (id: number) => ['projectCategories', id] as const,
};

export const useGetProjectCategoriesQuery = (id: number) =>
    useQuery<Category[], Error>({
        queryKey: ProjectCategoryKeys.projectCategories(id),
        queryFn: () => new CategoryApi().getProjectCategories({id}),
    });
