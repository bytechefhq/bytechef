import {
    IntegrationComponentElementI,
    getToolName,
} from '@/ee/pages/embedded/integrations/components/integration-list/integrationComponentElementUtils';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useMemo} from 'react';

const compareByTitle = (first: IntegrationComponentElementI, second: IntegrationComponentElementI) =>
    first.title.localeCompare(second.title);

const useIntegrationComponentElements = (componentName: string, componentVersion?: number, enabled = true) => {
    const {
        data: componentDefinition,
        isError,
        isLoading,
        refetch,
    } = useGetComponentDefinitionQuery(
        {componentName, componentVersion: componentVersion ?? 1},
        enabled && componentVersion !== undefined
    );

    const actions = useMemo<IntegrationComponentElementI[] | undefined>(
        () =>
            componentDefinition?.actions
                ?.map((action) => ({
                    description: action.description,
                    id: action.name,
                    title: action.title || action.name,
                }))
                .sort(compareByTitle),
        [componentDefinition]
    );

    const tools = useMemo<IntegrationComponentElementI[] | undefined>(
        () =>
            componentDefinition?.clusterElements
                ?.filter((clusterElement) => clusterElement.type === 'TOOLS')
                .map((clusterElement) => ({
                    description: clusterElement.description,
                    id: getToolName(clusterElement.componentName, clusterElement.name),
                    title: clusterElement.title || clusterElement.name,
                }))
                .sort(compareByTitle),
        [componentDefinition]
    );

    return {
        actions: componentDefinition ? actions || [] : undefined,
        isError,
        isLoading: isLoading || (enabled && componentVersion === undefined),
        refetch,
        tools: componentDefinition ? tools || [] : undefined,
    };
};

export default useIntegrationComponentElements;
