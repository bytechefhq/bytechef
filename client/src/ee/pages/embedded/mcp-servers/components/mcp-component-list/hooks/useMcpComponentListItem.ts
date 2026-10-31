import {useMcpComponentIntegrationsQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useMemo, useState} from 'react';

export default function useMcpComponentListItem(
    componentName: string,
    componentVersion: number,
    integrationId?: string | null
) {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName,
        componentVersion,
    });

    const {data: integrationsData} = useMcpComponentIntegrationsQuery({}, {enabled: !!integrationId});

    const integrationName = useMemo(
        () => integrationsData?.mcpComponentIntegrations.find((integration) => integration.id === integrationId)?.name,
        [integrationId, integrationsData]
    );

    return {
        componentDefinition,
        integrationName,
        setShowEditDialog,
        showEditDialog,
    };
}
