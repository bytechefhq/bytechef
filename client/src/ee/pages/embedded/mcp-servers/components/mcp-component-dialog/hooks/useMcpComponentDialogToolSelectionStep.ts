import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {useGetConnectionsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import {ComponentDefinition, McpComponent, McpTool, McpToolsByComponentIdQuery} from '@/shared/middleware/graphql';
import {ClusterElementDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo, useRef} from 'react';

export interface SelectedToolI {
    componentName: string;
    componentVersion: number;
    description?: string;
    name: string;
    title?: string;
}

const useMcpComponentDialogToolSelectionStep = ({
    existingTools,
    mcpComponent,
    onConnectionChange,
    onToolsChange,
    open,
    selectedComponent,
    selectedTools,
}: {
    existingTools?: McpToolsByComponentIdQuery;
    mcpComponent?: McpComponent;
    onConnectionChange: (connection: Connection | null) => void;
    onToolsChange: (tools: SelectedToolI[]) => void;
    open: boolean;
    selectedComponent: ComponentDefinition | null;
    selectedTools: SelectedToolI[];
}) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const selectAllCheckboxRef = useRef<HTMLButtonElement>(null);

    const {data: componentDefinition, isLoading: isLoadingComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: selectedComponent?.name || '',
            componentVersion: selectedComponent?.version || 1,
        },
        !!selectedComponent
    );

    const {data: connections = [], isLoading: isLoadingConnections} = useGetConnectionsQuery(
        {
            componentName: selectedComponent?.name,
            connectionVersion: selectedComponent?.version ?? undefined,
            environmentId: currentEnvironmentId,
        },
        open
    );

    const toolElements = useMemo(
        () => componentDefinition?.clusterElements?.filter((element) => element.type === 'TOOLS') || [],
        [componentDefinition?.clusterElements]
    );

    const handleToolToggle = (tool: ClusterElementDefinitionBasic, checked: boolean) => {
        if (checked) {
            onToolsChange([
                ...selectedTools,
                {
                    componentName: tool.componentName,
                    componentVersion: tool.componentVersion,
                    description: tool.description,
                    name: tool.name,
                    title: tool.title,
                },
            ]);
        } else {
            onToolsChange(selectedTools.filter((selectedTool) => selectedTool.name !== tool.name));
        }
    };

    const handleSelectAllTools = (checked: boolean) => {
        if (checked) {
            const allTools = toolElements.map((tool) => ({
                componentName: tool.componentName,
                componentVersion: tool.componentVersion,
                description: tool.description,
                name: tool.name,
                title: tool.title,
            }));

            onToolsChange(allTools);
        } else {
            onToolsChange([]);
        }
    };

    const allToolsSelected = toolElements.length > 0 && selectedTools.length === toolElements.length;
    const someToolsSelected = selectedTools.length > 0 && selectedTools.length < toolElements.length;

    useEffect(() => {
        if (selectAllCheckboxRef.current) {
            const checkboxElement = selectAllCheckboxRef.current.querySelector(
                'input[type="checkbox"]'
            ) as HTMLInputElement;

            if (checkboxElement) {
                checkboxElement.indeterminate = someToolsSelected;
            }
        }
    }, [someToolsSelected]);

    useEffect(() => {
        if (
            existingTools?.mcpToolsByComponentId &&
            existingTools?.mcpToolsByComponentId?.length > 0 &&
            toolElements.length > 0
        ) {
            const preSelectedTools = existingTools.mcpToolsByComponentId
                .map((existingTool: McpTool | null) => {
                    const toolElement = toolElements.find((tool) => tool.name === existingTool!.name);

                    if (toolElement) {
                        return {
                            componentName: toolElement.componentName,
                            componentVersion: toolElement.componentVersion,
                            description: toolElement.description,
                            name: existingTool!.name,
                            title: toolElement.title,
                        };
                    }

                    return null;
                })
                .filter(Boolean) as SelectedToolI[];

            onToolsChange(preSelectedTools);
        }
    }, [existingTools, toolElements, onToolsChange]);

    useEffect(() => {
        if (mcpComponent && connections.length > 0) {
            const existingConnection = connections.find(
                (connection) => connection.id?.toString() === mcpComponent.connectionId?.toString()
            );

            if (existingConnection) {
                onConnectionChange(existingConnection);
            }
        }
    }, [mcpComponent, connections, onConnectionChange]);

    return {
        allToolsSelected,
        connections,
        handleSelectAllTools,
        handleToolToggle,
        isLoadingComponentDefinition,
        isLoadingConnections,
        selectAllCheckboxRef,
        someToolsSelected,
        toolElements,
    };
};

export default useMcpComponentDialogToolSelectionStep;
