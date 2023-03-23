import {useQuery} from '@tanstack/react-query';
import {
    CategoryModel,
    IntegrationCategoriesApi,
    IntegrationModel,
    IntegrationTagsApi,
    IntegrationsApi,
    TagModel,
} from 'middleware/integration';

import {WorkflowModel} from '../middleware/workflow';

export const IntegrationKeys = {
    integration: (id: number) => ['integration', id],
    integrationCategories: ['integrationCategories'] as const,
    integrationList: (filters: {categoryIds?: number[]; tagIds?: number[]}) => [
        ...IntegrationKeys.integrations,
        filters,
    ],
    integrationTags: ['integrationTags'] as const,
    integrationWorkflows: (id: number) => [
        ...IntegrationKeys.integrations,
        id,
        'integrationWorkflows',
    ],
    integrations: ['integrations'] as const,
};

export const useGetIntegrationCategoriesQuery = () =>
    useQuery<CategoryModel[], Error>(
        IntegrationKeys.integrationCategories,
        () => new IntegrationCategoriesApi().getIntegrationCategories()
    );

export const useGetIntegrationTagsQuery = () =>
    useQuery<TagModel[], Error>(IntegrationKeys.integrationTags, () =>
        new IntegrationTagsApi().getIntegrationTags()
    );

export const useGetIntegrationQuery = (
    id: number,
    initialData?: IntegrationModel
) =>
    useQuery<IntegrationModel, Error>(
        IntegrationKeys.integration(id),
        () => new IntegrationsApi().getIntegration({id}),
        {
            initialData,
        }
    );

export const useGetIntegrationsQuery = (filters: {
    categoryIds?: number[];
    tagIds?: number[];
}) =>
    useQuery<IntegrationModel[], Error>(
        IntegrationKeys.integrationList(filters),
        () => new IntegrationsApi().getIntegrations(filters)
    );

export const useGetIntegrationWorkflowsQuery = (id: number) =>
    useQuery<WorkflowModel[], Error>(
        IntegrationKeys.integrationWorkflows(id),
        () => new IntegrationsApi().getIntegrationWorkflows({id})
    );
