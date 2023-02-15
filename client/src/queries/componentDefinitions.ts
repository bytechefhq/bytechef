import {useQuery} from '@tanstack/react-query';
import {
    ComponentDefinitionModel,
    ComponentDefinitionsApi,
} from '../data-access/component-definition';

export const ComponentDefinitionKeys = {
    componentDefinitions: ['componentDefinitions'] as const,
};

export const useGetComponentDefinitionsQuery = (filter?: {
    authenticationDefinitions?: boolean;
    authenticationInstances?: boolean;
}) =>
    useQuery<ComponentDefinitionModel[], Error>(
        ComponentDefinitionKeys.componentDefinitions,
        () => new ComponentDefinitionsApi().getComponentDefinitions(filter),
        {
            staleTime: 1 * 60 * 1000,
        }
    );
