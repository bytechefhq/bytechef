import {useQuery} from '@tanstack/react-query';
import {
    ActionDefinitionModel,
    ActionDefinitionsApi,
    GetComponentActionDefinitionRequest,
} from 'middleware/definition-registry';

export const ActionDefinitionKeys = {
    actionDefinition: (request: GetComponentActionDefinitionRequest) => [
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
