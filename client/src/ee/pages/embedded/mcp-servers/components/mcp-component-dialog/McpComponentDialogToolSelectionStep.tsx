import LoadingIcon from '@/components/LoadingIcon';
import {Checkbox} from '@/components/ui/checkbox';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentDefinition, McpComponent, McpToolsByComponentIdQuery} from '@/shared/middleware/graphql';

import useMcpComponentDialogToolSelectionStep, {SelectedToolI} from './hooks/useMcpComponentDialogToolSelectionStep';

interface ToolSelectionStepProps {
    existingTools?: McpToolsByComponentIdQuery;
    mcpComponent?: McpComponent;
    onConnectionChange: (connection: Connection | null) => void;
    onToolsChange: (tools: SelectedToolI[]) => void;
    open: boolean;
    selectedComponent: ComponentDefinition | null;
    selectedConnection: Connection | null;
    selectedTools: SelectedToolI[];
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
    const {
        allToolsSelected,
        connections,
        handleSelectAllTools,
        handleToolToggle,
        isLoadingComponentDefinition,
        isLoadingConnections,
        selectAllCheckboxRef,
        toolElements,
    } = useMcpComponentDialogToolSelectionStep({
        existingTools,
        mcpComponent,
        onConnectionChange,
        onToolsChange,
        open,
        selectedComponent,
        selectedTools,
    });

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
                            const connection = connections.find(
                                (connectionItem) => connectionItem.id?.toString() === value
                            );

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
                <div className="flex items-center justify-center py-8">
                    <LoadingIcon className="size-6" />
                </div>
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
                                    checked={selectedTools.some((selectedTool) => selectedTool.name === tool.name)}
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
