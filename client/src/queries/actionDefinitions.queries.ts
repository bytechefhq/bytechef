/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';
import {
    ActionDefinitionApi,
    ActionDefinitionModel,
    GetActionDefinitionsRequest,
    GetComponentActionDefinitionRequest,
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
};

export const useGetActionDefinitionQuery = (
    request: GetComponentActionDefinitionRequest,
    enabledCondition?: boolean
) =>
    useQuery<ActionDefinitionModel, Error>({
        queryKey: ActionDefinitionKeys.actionDefinition(request),
        queryFn: () =>
            new ActionDefinitionApi().getComponentActionDefinition(request),
        enabled: false || enabledCondition,
    });

export const useGetActionDefinitionsQuery = (
    request: GetActionDefinitionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ActionDefinitionModel[], Error>({
        queryKey: ActionDefinitionKeys.filteredActionDefinitions(request),
        queryFn: () => new ActionDefinitionApi().getActionDefinitions(request),
        enabled: false || enabledCondition,
    });
