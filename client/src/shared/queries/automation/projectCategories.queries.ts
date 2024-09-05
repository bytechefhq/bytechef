/* eslint-disable sort-keys */
import {Category, CategoryApi} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectCategoryKeys = {
    projectCategories: ['projectCategories'] as const,
};

export const useGetProjectCategoriesQuery = () =>
    useQuery<Category[], Error>({
        queryKey: ProjectCategoryKeys.projectCategories,
        queryFn: () => new CategoryApi().getProjectCategories(),
    });
