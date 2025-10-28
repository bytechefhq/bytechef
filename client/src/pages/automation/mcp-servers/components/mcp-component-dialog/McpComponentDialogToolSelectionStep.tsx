import {Checkbox} from '@/components/ui/checkbox';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Connection} from '@/shared/middleware/automation/configuration';
import {McpComponent, McpTool, McpToolsByComponentIdQuery} from '@/shared/middleware/graphql';
import {ClusterElementDefinitionBasic, ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetWorkspaceConnectionsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useMemo, useRef} from 'react';

interface SelectedToolI {
    name: string;
    componentName: string;
    componentVersion: number;
    title?: string;
    description?: string;
}

interface ToolSelectionStepProps {
    open: boolean;
    mcpComponent?: McpComponent;
    selectedComponent: ComponentDefinitionBasic | null;
    selectedTools: SelectedToolI[];
    selectedConnection: Connection | null;
    onToolsChange: (tools: SelectedToolI[]) => void;
    onConnectionChange: (connection: Connection | null) => void;
    existingTools?: McpToolsByComponentIdQuery;
}

const McpComponentDialogToolSelectionStep = ({
    existingTools,
    mcpComponent,
    onConnectionChange,
    onToolsChange,
    open,
    selectedComponent,
    selectedConnection,
    selectedTools,
}: ToolSelectionStepProps) => {
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
            onToolsChange(selectedTools.filter((t) => t.name !== tool.name));
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
                (conn) => conn.id?.toString() === mcpComponent.connectionId?.toString()
            );
            if (existingConnection) {
                onConnectionChange(existingConnection);
            }
        }
    }, [mcpComponent, connections, onConnectionChange]);

    return (
        <div className="space-y-4 py-4">
            <div className="space-y-2">
                <Label className="text-sm font-medium" htmlFor="connection-select">
                    Select Connection
                </Label>

                <Select
                    onValueChange={(value) => {
                        if (value === 'no-connection') {
                            onConnectionChange(null);
                        } else {
                            const connection = connections.find((conn) => conn.id?.toString() === value);
                            onConnectionChange(connection || null);
                        }
                    }}
                    value={selectedConnection?.id?.toString() || 'no-connection'}
                >
                    <SelectTrigger id="connection-select">
                        <SelectValue placeholder="Choose a connection..." />
                    </SelectTrigger>

                    <SelectContent>
                        <SelectItem value="no-connection">No connection</SelectItem>

                        {isLoadingConnections ? (
                            <SelectItem disabled value="loading">
                                Loading connections...
                            </SelectItem>
                        ) : (
                            connections.map((connection) => (
                                <SelectItem key={connection.id} value={connection.id?.toString() || 'no-connection'}>
                                    {connection.name}
                                </SelectItem>
                            ))
                        )}
                    </SelectContent>
                </Select>
            </div>

            {isLoadingComponentDefinition ? (
                <div className="py-8 text-center">Loading tools...</div>
            ) : toolElements.length === 0 ? (
                <div className="py-8 text-center text-muted-foreground">No tools available for this component.</div>
            ) : (
                <>
                    <div className="flex items-center space-x-3">
                        <Checkbox
                            checked={allToolsSelected}
                            id="select-all-tools"
                            onCheckedChange={(checked) => handleSelectAllTools(checked as boolean)}
                            ref={selectAllCheckboxRef}
                        />

                        <label className="cursor-pointer text-sm font-medium leading-none" htmlFor="select-all-tools">
                            Select All Tools ({toolElements.length})
                        </label>
                    </div>

                    <div className="divide-y">
                        {toolElements.map((tool) => (
                            <div className="flex items-center space-x-3 py-3 hover:bg-gray-50" key={tool.name}>
                                <Checkbox
                                    checked={selectedTools.some((t) => t.name === tool.name)}
                                    id={tool.name}
                                    onCheckedChange={(checked) => handleToolToggle(tool, checked as boolean)}
                                />

                                <div className="flex-1">
                                    <label className="cursor-pointer text-sm font-medium" htmlFor={tool.name}>
                                        {tool.title || tool.name}
                                    </label>

                                    {tool.description && (
                                        <p className="mt-1 text-sm text-muted-foreground">{tool.description}</p>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
};

export default McpComponentDialogToolSelectionStep;
