import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {PopoverContent} from '@/components/ui/popover';
import useAiHubConnectorToolPropertiesPopover from '@/ee/pages/automation/ai-hub/context/hooks/useAiHubConnectorToolPropertiesPopover';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {XIcon} from 'lucide-react';
import {useMemo} from 'react';

interface ConnectorToolI {
    name: string;
    parameters?: unknown;
    title?: string | null;
}

interface AiHubConnectorToolPropertiesPopoverProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectorId: string;
    onClose: () => void;
    tool: ConnectorToolI;
    workspaceId: string;
}

/**
 * Per-tool "Configure" popover on the Connectors page — the AI Hub counterpart of the MCP server's
 * McpComponentToolPropertiesPopover. Renders the tool's input properties and persists pre-set parameter values
 * the agent will use when it calls the tool.
 */
const AiHubConnectorToolPropertiesPopover = ({
    componentName,
    componentVersion,
    connectionId,
    connectorId,
    onClose,
    tool,
    workspaceId,
}: AiHubConnectorToolPropertiesPopoverProps) => {
    const {control, form, formState, handleFormSubmit, handleSubmit, isLoading, properties} =
        useAiHubConnectorToolPropertiesPopover(
            componentName,
            componentVersion,
            connectorId,
            tool,
            workspaceId,
            onClose
        );

    const formValues = form.watch();

    // Properties (the workflow-editor renderer) reads the WorkflowReadOnly context for component-definition
    // lookups; without this provider it throws "useWorkflowEditor must be used within a provider". Same value
    // the MCP server tool list supplies.
    const workflowReadOnlyValue = useMemo(() => ({useGetComponentDefinitionsQuery}), []);

    return (
        <PopoverContent
            align="end"
            className="flex max-h-(--radix-popover-content-available-height) w-md flex-col p-0"
            onInteractOutside={(event) => event.preventDefault()}
            side="bottom"
        >
            <div className="flex items-center justify-between p-3">
                <div>
                    <h4 className="text-sm font-medium">{`Configure ${tool.title || tool.name}`}</h4>

                    <p className="text-xs text-muted-foreground">Set property values the agent uses for this tool.</p>
                </div>

                <Button
                    className="text-muted-foreground hover:text-foreground"
                    icon={<XIcon className="size-4" />}
                    onClick={onClose}
                    size="iconXs"
                    variant="ghost"
                />
            </div>

            <Form {...form}>
                <form className="flex min-h-0 flex-col" onSubmit={handleSubmit(handleFormSubmit)}>
                    <div className="min-h-0 flex-1 overflow-y-auto p-3">
                        {isLoading ? (
                            <p className="text-sm text-muted-foreground">Loading properties...</p>
                        ) : properties.length > 0 ? (
                            <fieldset className="space-y-4 border-0 p-0">
                                <WorkflowReadOnlyProvider value={workflowReadOnlyValue}>
                                    <ClusterElementProvider
                                        value={{
                                            clusterElementName: tool.name,
                                            componentName,
                                            componentVersion,
                                            connectionId: connectionId ? Number(connectionId) : undefined,
                                            inputParameters: formValues,
                                        }}
                                    >
                                        <Properties
                                            control={control}
                                            controlPath=""
                                            formState={formState}
                                            properties={properties}
                                            toolsMode
                                        />
                                    </ClusterElementProvider>
                                </WorkflowReadOnlyProvider>
                            </fieldset>
                        ) : (
                            <p className="text-sm text-muted-foreground">No configurable properties for this tool.</p>
                        )}
                    </div>

                    <div className="flex justify-end gap-2 p-3">
                        <Button label="Cancel" onClick={onClose} type="button" variant="outline" />

                        <Button disabled={properties.length === 0} label="Save" type="submit" />
                    </div>
                </form>
            </Form>
        </PopoverContent>
    );
};

export default AiHubConnectorToolPropertiesPopover;
