import {
    ComponentDefinitionApi,
    ComponentDefinitionBasicModel,
    ComponentDefinitionModel,
    GetComponentDefinitionRequest,
    GetComponentDefinitionsRequest,
} from '@/middleware/hermes/configuration';
import {useQuery} from '@tanstack/react-query';

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
        () => new ComponentDefinitionApi().getComponentDefinition(request),
        {
            enabled: !!request.componentName,
        }
    );

export const useGetComponentDefinitionsQuery = (
    request?: GetComponentDefinitionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ComponentDefinitionBasicModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions(request),
        () => new ComponentDefinitionApi().getComponentDefinitions(request),
        {
            enabled: false || enabledCondition,
        }
    );
