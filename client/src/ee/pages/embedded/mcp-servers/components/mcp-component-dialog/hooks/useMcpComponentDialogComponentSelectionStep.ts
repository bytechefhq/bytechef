import {useMcpComponentDefinitionsQuery, useMcpComponentIntegrationsQuery} from '@/shared/middleware/graphql';
import {useMemo, useState} from 'react';

import {McpIntegrationComponentOptionI} from './useMcpComponentDialog';

const useMcpComponentDialogComponentSelectionStep = (open: boolean, componentName?: string) => {
    const [searchTerm, setSearchTerm] = useState('');

    const {data: componentDefinitionsData, isLoading: isLoadingComponentDefinitions} = useMcpComponentDefinitionsQuery(
        {},
        {enabled: open}
    );

    const {data: integrationsData, isLoading: isLoadingIntegrations} = useMcpComponentIntegrationsQuery(
        {},
        {enabled: open}
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
        isLoadingComponents: isLoadingComponentDefinitions || isLoadingIntegrations,
        searchTerm,
        setSearchTerm,
    };
};

export default useMcpComponentDialogComponentSelectionStep;
