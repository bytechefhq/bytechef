import {useQuery} from '@tanstack/react-query';
import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionsApi,
} from '../middleware/definition-registry';

interface Request {
    connectionDefinitions?: boolean;
    connectionInstances?: boolean;
}

export const ComponentDefinitionKeys = {
    componentDefinitions: (request?: Request) => [
        'componentDefinitions',
        request,
    ],
};

export const useGetComponentDefinitionsQuery = (request?: Request) =>
    useQuery<ComponentDefinitionBasicModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions(request),
        () => new ComponentDefinitionsApi().getComponentDefinitions(request),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
