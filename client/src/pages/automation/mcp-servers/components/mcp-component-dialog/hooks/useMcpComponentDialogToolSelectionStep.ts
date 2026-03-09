import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Connection} from '@/shared/middleware/automation/configuration';
import {McpComponent, McpTool, McpToolsByComponentIdQuery} from '@/shared/middleware/graphql';
import {ClusterElementDefinitionBasic, ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo, useRef} from 'react';

export type SelectedToolType = {
    name: string;
    componentName: string;
    componentVersion: number;
    title?: string;
    description?: string;
};

interface UseMcpComponentDialogToolSelectionStepProps {
    open: boolean;
    mcpComponent?: McpComponent;
    selectedComponent: ComponentDefinitionBasic | null;
    selectedTools: SelectedToolType[];
    onToolsChange: (tools: SelectedToolType[]) => void;
    onConnectionChange: (connection: Connection | null) => void;
    existingTools?: McpToolsByComponentIdQuery;
}

const useMcpComponentDialogToolSelectionStep = ({
    existingTools,
    mcpComponent,
    onConnectionChange,
    onToolsChange,
    open,
    selectedComponent,
    selectedTools,
}: UseMcpComponentDialogToolSelectionStepProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const selectAllCheckboxRef = useRef<HTMLButtonElement>(null);

    const {data: componentDefinition, isLoading: isLoadingComponentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: selectedComponent?.name || '',
            componentVersion: selectedComponent?.version || 1,
        },
        !!selectedComponent
    );

    const {data: connections = [], isLoading: isLoadingConnections} = useGetWorkspaceConnectionsQuery(
        {
            componentName: selectedComponent?.name,
            connectionVersion: selectedComponent?.version,
            environmentId: currentEnvironmentId,
            id: currentWorkspaceId!,
        },
        open && !!currentWorkspaceId && !!selectedComponent
    );

    const toolElements = useMemo(
        () => componentDefinition?.clusterElements?.filter((element) => element.type === 'TOOLS') || [],
        [componentDefinition?.clusterElements]
    );

    const handleToolToggle = (tool: ClusterElementDefinitionBasic, checked: boolean) => {
        if (!checked) {
            onToolsChange(selectedTools.filter((selectedTool) => selectedTool.name !== tool.name));

            return;
        }

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
    };

    const handleSelectAllTools = (checked: boolean) => {
        if (!checked) {
            onToolsChange([]);

            return;
        }

        const allTools = toolElements.map((tool) => ({
            componentName: tool.componentName,
            componentVersion: tool.componentVersion,
            description: tool.description,
            name: tool.name,
            title: tool.title,
        }));

        onToolsChange(allTools);
    };

    const allToolsSelected = toolElements.length > 0 && selectedTools.length === toolElements.length;
    const someToolsSelected = selectedTools.length > 0 && selectedTools.length < toolElements.length;

    useEffect(() => {
        if (!selectAllCheckboxRef.current) {
            return;
        }

        const checkboxElement = selectAllCheckboxRef.current.querySelector(
            'input[type="checkbox"]'
        ) as HTMLInputElement;

        if (checkboxElement) {
            checkboxElement.indeterminate = someToolsSelected;
        }
    }, [someToolsSelected]);

    const hasExistingToolsAndElements =
        existingTools?.mcpToolsByComponentId &&
        existingTools?.mcpToolsByComponentId?.length > 0 &&
        toolElements.length > 0;

    useEffect(() => {
        if (!hasExistingToolsAndElements) {
            return;
        }

        const validExistingTools = existingTools.mcpToolsByComponentId!.filter(
            (existingTool): existingTool is McpTool => existingTool !== null
        );

        const matchedTools = validExistingTools.map((existingTool) => {
            const toolElement = toolElements.find((tool) => tool.name === existingTool.name);

            if (!toolElement) {
                return null;
            }

            return {
                componentName: toolElement.componentName,
                componentVersion: toolElement.componentVersion,
                description: toolElement.description,
                name: existingTool.name,
                title: toolElement.title,
            };
        });

        const preSelectedTools = matchedTools.filter(Boolean) as SelectedToolType[];

        onToolsChange(preSelectedTools);
    }, [existingTools?.mcpToolsByComponentId, hasExistingToolsAndElements, onToolsChange, toolElements]);

    useEffect(() => {
        if (!mcpComponent || connections.length === 0) {
            return;
        }

        const existingConnection = connections.find(
            (connection) => connection.id?.toString() === mcpComponent.connectionId?.toString()
        );

        if (existingConnection) {
            onConnectionChange(existingConnection);
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
