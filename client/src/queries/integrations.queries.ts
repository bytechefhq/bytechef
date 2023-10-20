import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    IntegrationModel,
    IntegrationsApi,
    TagModel,
} from 'data-access/integration';

export enum ServerStateKeysEnum {
    IntegrationCategories = 'integrationCategories',
    IntegrationTags = 'integrationTags',
    Integrations = 'integrations',
}

export const useGetIntegrationCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(
        [ServerStateKeysEnum.IntegrationCategories],
        () => new IntegrationsApi().getIntegrationCategories()
    );

export const useGetIntegrationTagsQuery = () =>
    useQuery<TagModel[], Error>([ServerStateKeysEnum.IntegrationTags], () =>
        new IntegrationsApi().getIntegrationTags()
    );

export const useGetIntegrationsQuery = () =>
    useQuery<IntegrationModel[], Error>(
        [ServerStateKeysEnum.Integrations],
        () => new IntegrationsApi().getIntegrations()
    );
