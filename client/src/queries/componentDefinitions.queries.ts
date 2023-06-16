import {useQuery} from '@tanstack/react-query';

import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
    ComponentDefinitionsApi,
    GetComponentDefinitionRequest,
    GetComponentDefinitionsRequest,
} from '../middleware/core/definition-registry';

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
    useQuery<ComponentDefinitionModel, Error>(
        ComponentDefinitionKeys.componentDefinition(request),
        () => new ComponentDefinitionsApi().getComponentDefinition(request),
        {
            enabled: !!request.componentName,
        }
    );

export const useGetComponentDefinitionsQuery = (
    request?: GetComponentDefinitionsRequest
) =>
    useQuery<ComponentDefinitionBasicModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions(request),
        () => new ComponentDefinitionsApi().getComponentDefinitions(request)
    );
