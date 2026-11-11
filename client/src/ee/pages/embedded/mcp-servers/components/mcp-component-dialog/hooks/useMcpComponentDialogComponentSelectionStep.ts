import {
    McpComponent,
    useEmbeddedMcpComponentsByServerIdQuery,
    useMcpComponentDefinitionsQuery,
    useMcpComponentIntegrationsQuery,
} from '@/shared/middleware/graphql';
import {useCallback, useMemo, useState} from 'react';

import {McpIntegrationComponentOptionI} from './useMcpComponentDialog';

const getComponentKey = (name: string, version: number | null | undefined) => `${name}/${version ?? 1}`;

const useMcpComponentDialogComponentSelectionStep = (
    open: boolean,
    mcpServerId: string,
    mcpComponent?: McpComponent
) => {
    const [searchTerm, setSearchTerm] = useState('');

    const componentName = mcpComponent?.componentName;

    const {data: componentDefinitionsData, isLoading: isLoadingComponentDefinitions} = useMcpComponentDefinitionsQuery(
        {},
        {enabled: open}
    );

    const {data: integrationsData, isLoading: isLoadingIntegrations} = useMcpComponentIntegrationsQuery(
        {},
        {enabled: open}
    );

    const {data: mcpComponentsData, isLoading: isLoadingMcpComponents} = useEmbeddedMcpComponentsByServerIdQuery(
        {mcpServerId},
        {enabled: open && !!mcpServerId}
    );

    const addedComponentKeys = useMemo(
        () =>
            new Set(
                (mcpComponentsData?.embeddedMcpComponentsByServerId ?? [])
                    .filter(
                        (serverMcpComponent) => serverMcpComponent != null && serverMcpComponent.id !== mcpComponent?.id
                    )
                    .map((serverMcpComponent) =>
                        getComponentKey(serverMcpComponent!.componentName, serverMcpComponent!.componentVersion)
                    )
            ),
        [mcpComponent?.id, mcpComponentsData]
    );

    const isComponentAdded = useCallback(
        (integrationComponentOption: McpIntegrationComponentOptionI) =>
            addedComponentKeys.has(
                getComponentKey(integrationComponentOption.name, integrationComponentOption.version)
            ),
        [addedComponentKeys]
    );

    const integrationComponentOptions = useMemo(() => {
        const componentDefinitions = componentDefinitionsData?.mcpComponentDefinitions ?? [];

        return (integrationsData?.mcpComponentIntegrations ?? [])
            .filter((integration) => !componentName || integration.componentName === componentName)
            .flatMap((integration): McpIntegrationComponentOptionI[] => {
                const componentDefinition = componentDefinitions.find(
                    (definition) => definition.name === integration.componentName
                );

                if (!componentDefinition) {
                    return [];
                }

                return [
                    {
                        ...componentDefinition,
                        id: integration.id,
                        integrationId: integration.id,
                        title: integration.name,
                        version: componentDefinition.version ?? null,
                    },
                ];
            });
    }, [componentDefinitionsData, componentName, integrationsData]);

    const filteredComponents = integrationComponentOptions.filter((integrationComponentOption) => {
        const searchLower = searchTerm.toLowerCase();
        const nameMatch = integrationComponentOption.name.toLowerCase().includes(searchLower);
        const titleMatch = integrationComponentOption.title?.toLowerCase().includes(searchLower);

        return nameMatch || titleMatch;
    });

    return {
        filteredComponents,
        isComponentAdded,
        isLoadingComponents: isLoadingComponentDefinitions || isLoadingIntegrations || isLoadingMcpComponents,
        searchTerm,
        setSearchTerm,
    };
};

export default useMcpComponentDialogComponentSelectionStep;
