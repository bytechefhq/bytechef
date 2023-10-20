import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    GetIntegrationsRequest,
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
        () => new IntegrationsApi().getIntegrationCategories(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationTagsQuery = () =>
    useQuery<TagModel[], Error>(
        [ServerStateKeysEnum.IntegrationTags],
        () => new IntegrationsApi().getIntegrationTags(),
        {
            staleTime: 1 * 60 * 1000,
        }
    );

export const useGetIntegrationsQuery = (
    requestParameters?: GetIntegrationsRequest
) =>
    useQuery<IntegrationModel[], Error>(
        [ServerStateKeysEnum.Integrations, requestParameters],
        () => new IntegrationsApi().getIntegrations(requestParameters),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
