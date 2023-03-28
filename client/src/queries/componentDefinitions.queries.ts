import {useQuery} from '@tanstack/react-query';

import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionsApi,
    GetComponentDefinitionsRequest,
} from '../middleware/definition-registry';

export const ComponentDefinitionKeys = {
    componentDefinitions: (request?: GetComponentDefinitionsRequest) => [
        'componentDefinitions',
        request,
    ],
};

export const useGetComponentDefinitionsQuery = (
    request?: GetComponentDefinitionsRequest
) =>
    useQuery<ComponentDefinitionBasicModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions(request),
        () => new ComponentDefinitionsApi().getComponentDefinitions(request)
    );
