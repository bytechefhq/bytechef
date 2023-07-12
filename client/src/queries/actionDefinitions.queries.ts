import {useQuery} from '@tanstack/react-query';
import {
    ActionDefinitionModel,
    ActionDefinitionsApi,
    GetActionDefinitionsRequest,
    GetComponentActionDefinitionRequest,
} from 'middleware/hermes/configuration';

export const ActionDefinitionKeys = {
    actionDefinition: (request: GetComponentActionDefinitionRequest) => [
        'actionDefinition',
        request,
    ],
    actionDefinitions: (request: GetActionDefinitionsRequest) => [
        'actionDefinitions',
        request,
    ],
};

export const useGetActionDefinitionQuery = (
    request: GetComponentActionDefinitionRequest,
    enabledCondition?: boolean
) =>
    useQuery<ActionDefinitionModel, Error>(
        ActionDefinitionKeys.actionDefinition(request),
        () => new ActionDefinitionsApi().getComponentActionDefinition(request),
        {enabled: false || enabledCondition}
    );

export const useGetActionDefinitionsQuery = (
    request: GetActionDefinitionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ActionDefinitionModel[], Error>(
        ActionDefinitionKeys.actionDefinitions(request),
        () => new ActionDefinitionsApi().getActionDefinitions(request),
        {enabled: false || enabledCondition}
    );
