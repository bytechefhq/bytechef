import {useQuery} from '@tanstack/react-query';

import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionWithBasicActionsModel,
    ComponentDefinitionsApi,
    GetComponentDefinitionRequest,
    GetComponentDefinitionsRequest,
} from '../middleware/definition-registry';

export const ComponentDefinitionKeys = {
    componentDefinition: (request: GetComponentDefinitionRequest) => [
        'componentDefinition',
        request,
    ],
    componentDefinitions: (request?: GetComponentDefinitionsRequest) => [
        'componentDefinitions',
        request,
    ],
};

export const useGetComponentDefinitionQuery = (
    request: GetComponentDefinitionRequest
) =>
    useQuery<ComponentDefinitionWithBasicActionsModel, Error>(
        ComponentDefinitionKeys.componentDefinition(request),
        () => new ComponentDefinitionsApi().getComponentDefinition(request)
    );

export const useGetComponentDefinitionsQuery = (
    request?: GetComponentDefinitionsRequest
) =>
    useQuery<ComponentDefinitionBasicModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions(request),
        () => new ComponentDefinitionsApi().getComponentDefinitions(request)
    );
