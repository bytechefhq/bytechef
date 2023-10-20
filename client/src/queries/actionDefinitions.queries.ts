import {useQuery} from '@tanstack/react-query';
import {
    ActionDefinitionModel,
    ActionDefinitionsApi,
    GetComponentActionDefinitionRequest,
} from 'middleware/definition-registry';

export const ActionDefinitionKeys = {
    actionDefinition: (
        queryKey = 'actionDefinition',
        request: GetComponentActionDefinitionRequest
    ) => [queryKey, request],
};

export const useGetActionDefinitionQuery = (
    request: GetComponentActionDefinitionRequest,
    queryKey?: string,
    enabledCondition?: boolean
) =>
    useQuery<ActionDefinitionModel, Error>(
        ActionDefinitionKeys.actionDefinition(queryKey, request),
        () => new ActionDefinitionsApi().getComponentActionDefinition(request),
        {enabled: false || enabledCondition}
    );
