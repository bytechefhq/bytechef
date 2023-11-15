/* eslint-disable sort-keys */
import {CategoryApi, CategoryModel} from '@/middleware/helios/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectCategoryKeys = {
    projectCategories: ['projectCategories'] as const,
};

export const useGetProjectCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>({
        queryKey: ProjectCategoryKeys.projectCategories,
        queryFn: () => new CategoryApi().getProjectCategories(),
    });
