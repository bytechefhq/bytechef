import {ComponentDefinitionBasic} from '@/shared/middleware/automation/configuration';
import {useMcpComponentsByServerIdQuery} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useCallback, useMemo, useState} from 'react';

const getComponentKey = (name: string, version: number | null | undefined) => `${name}/${version}`;

const useMcpComponentDialogComponentSelectionStep = (open: boolean, mcpServerId: string) => {
    const [searchTerm, setSearchTerm] = useState('');

    const {data: components = [], isLoading: isLoadingComponents} = useGetComponentDefinitionsQuery(
        {
            actionDefinitions: true,
        },
        open
    );

    const {data: mcpComponentsData, isLoading: isLoadingMcpComponents} = useMcpComponentsByServerIdQuery(
        {mcpServerId},
        {enabled: open && !!mcpServerId}
    );

    const addedComponentKeys = useMemo(
        () =>
            new Set(
                (mcpComponentsData?.mcpComponentsByServerId ?? [])
                    .filter((mcpComponent) => mcpComponent != null)
                    .map((mcpComponent) => getComponentKey(mcpComponent!.componentName, mcpComponent!.componentVersion))
            ),
        [mcpComponentsData]
    );

    const isComponentAdded = useCallback(
        (component: ComponentDefinitionBasic) =>
            addedComponentKeys.has(getComponentKey(component.name, component.version)),
        [addedComponentKeys]
    );

    const filteredComponents = components.filter((component) => {
        const clusterElementsCount = component.clusterElementsCount ?? {};

        if (!clusterElementsCount.TOOLS) {
            return false;
        }

        if (component.clusterRoot) {
            return false;
        }

        const searchLower = searchTerm.toLowerCase();

        const nameMatch = component.name.toLowerCase().includes(searchLower);
        const titleMatch = component.title?.toLowerCase().includes(searchLower);

        return nameMatch || titleMatch;
    });

    return {
        filteredComponents,
        isComponentAdded,
        isLoadingComponents: isLoadingComponents || isLoadingMcpComponents,
        searchTerm,
        setSearchTerm,
    };
};

export default useMcpComponentDialogComponentSelectionStep;
