import {
    ActionDefinition,
    ActionDefinitionApi,
    type ActionDefinitionBasic,
    GetComponentActionDefinitionRequest,
    GetComponentActionDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ActionDefinitionKeys = {
    actionDefinition: (request: GetComponentActionDefinitionRequest) => [
        ...ActionDefinitionKeys.actionDefinitions,
        request.componentName,
        request.componentVersion,
        request.actionName,
    ],
    filteredActionDefinitions: (request: GetComponentActionDefinitionsRequest) => [
        ...ActionDefinitionKeys.actionDefinitions,
        request.componentName,
        request.componentVersion,
    ],
    actionDefinitions: ['actionDefinitions'] as const,
};

export const useGetComponentActionDefinitionQuery = (request: GetComponentActionDefinitionRequest, enabled?: boolean) =>
    useQuery<ActionDefinitionBasic, Error>({
        queryKey: ActionDefinitionKeys.actionDefinition(request),
        queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: DEFINITION_STALE_TIME,
    });

export const useGetComponentActionDefinitionsQuery = (
    request: GetComponentActionDefinitionsRequest,
    enabled?: boolean
) =>
    useQuery<ActionDefinition[], Error>({
        queryKey: ActionDefinitionKeys.filteredActionDefinitions(request),
        queryFn: () => new ActionDefinitionApi().getComponentActionDefinitions(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: DEFINITION_STALE_TIME,
    });
