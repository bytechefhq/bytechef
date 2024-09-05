/* eslint-disable sort-keys */
import {Category, CategoryApi} from '@/shared/middleware/embedded/configuration';
import {useQuery} from '@tanstack/react-query';

export const IntegrationCategoryKeys = {
    integrationCategories: ['integrationCategories'] as const,
};

export const useGetIntegrationCategoriesQuery = () =>
    useQuery<Category[], Error>({
        queryKey: IntegrationCategoryKeys.integrationCategories,
        queryFn: () => new CategoryApi().getIntegrationCategories(),
    });
