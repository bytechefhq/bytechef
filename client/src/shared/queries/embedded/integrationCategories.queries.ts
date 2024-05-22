/* eslint-disable sort-keys */
import {CategoryApi, CategoryModel} from '@/shared/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const IntegrationCategoryKeys = {
    integrationCategories: ['integrationCategories'] as const,
};

export const useGetIntegrationCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>({
        queryKey: IntegrationCategoryKeys.integrationCategories,
        queryFn: () => new CategoryApi().getIntegrationCategories(),
    });
