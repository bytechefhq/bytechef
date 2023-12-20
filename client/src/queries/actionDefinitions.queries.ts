/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';
import {
    ActionDefinitionApi,
    ActionDefinitionModel,
    GetActionDefinitionsRequest,
    GetComponentActionDefinitionRequest,
    GetComponentActionOutputSchemaRequest,
    type OutputSchemaResponseModel,
} from 'middleware/hermes/configuration';

export const ActionDefinitionKeys = {
    actionDefinition: (request: GetComponentActionDefinitionRequest) => [
        ...ActionDefinitionKeys.actionDefinitions,
        request,
    ],
    actionDefinitions: ['actionDefinitions'] as const,
    filteredActionDefinitions: (request: GetActionDefinitionsRequest) => [
        ...ActionDefinitionKeys.actionDefinitions,
        request,
    ],
    filteredActionOutputSchemas: (request: GetComponentActionOutputSchemaRequest) => [
        ...ActionDefinitionKeys.actionDefinitions,
        'outputSchemas',
        request,
    ],
};

export const useGetActionDefinitionsQuery = (request: GetActionDefinitionsRequest, enabled?: boolean) =>
    useQuery<ActionDefinitionModel[], Error>({
        queryKey: ActionDefinitionKeys.filteredActionDefinitions(request),
        queryFn: () => new ActionDefinitionApi().getActionDefinitions(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: 60 * 1000,
    });

export const useGetComponentActionDefinitionQuery = (request: GetComponentActionDefinitionRequest, enabled?: boolean) =>
    useQuery<ActionDefinitionModel, Error>({
        queryKey: ActionDefinitionKeys.actionDefinition(request),
        queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetComponentActionOutputSchemaQuery = (
    request: GetComponentActionOutputSchemaRequest,
    enabled?: boolean
) =>
    useQuery<OutputSchemaResponseModel, Error>({
        queryKey: ActionDefinitionKeys.filteredActionOutputSchemas(request),
        queryFn: () => new ActionDefinitionApi().getComponentActionOutputSchema(request),
        enabled: enabled === undefined ? true : enabled,
    });
