import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    IntegrationModel,
    IntegrationsApi,
    TagModel,
} from 'data-access/integration';

export const IntegrationsKeys = {
    integrationCategories: ['integrationCategories'] as const,
    integrationTags: ['integrationTags'] as const,
    integrations: ['integrations'] as const,
    integrationList: (filters: {categoryId?: number; tagId?: number}) => [
        ...IntegrationsKeys.integrations,
        filters,
    ],
};

export const useGetIntegrationCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(
        IntegrationsKeys.integrationCategories,
        () => new IntegrationsApi().getIntegrationCategories(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationTagsQuery = () =>
    useQuery<TagModel[], Error>(
        IntegrationsKeys.integrationTags,
        () => new IntegrationsApi().getIntegrationTags(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationsQuery = (filters: {
    categoryId?: number;
    tagId?: number;
}) =>
    useQuery<IntegrationModel[], Error>(
        IntegrationsKeys.integrationList(filters),
        () => new IntegrationsApi().getIntegrations(filters),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
