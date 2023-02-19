import {useQuery} from '@tanstack/react-query';
import {
    ComponentDefinitionBasicModel,
    ComponentDefinitionsApi,
} from '../data-access/definition-registry';

export const ComponentDefinitionKeys = {
    componentDefinitions: ['componentDefinitions'] as const,
};

export const useGetComponentDefinitionsQuery = (filter?: {
    connectionDefinitions?: boolean;
    connectionInstances?: boolean;
}) =>
    useQuery<ComponentDefinitionBasicModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions,
        () => new ComponentDefinitionsApi().getComponentDefinitions(filter),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
