import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {PopoverContent} from '@/components/ui/popover';
import useMcpComponentToolPropertiesPopover from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentToolPropertiesPopover';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {McpTool} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';

interface McpComponentToolPropertiesPopoverProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    mcpTool: McpTool;
    onClose: () => void;
}

const McpComponentToolPropertiesPopover = ({
    componentName,
    componentVersion,
    connectionId,
    mcpTool,
    onClose,
}: McpComponentToolPropertiesPopoverProps) => {
    const {control, form, formState, handleFormSubmit, handleSubmit, isLoading, properties} =
        useMcpComponentToolPropertiesPopover(componentName, componentVersion, mcpTool, onClose);

    const formValues = form.watch();

    return (
        <PopoverContent
            align="start"
            className="flex max-h-[var(--radix-popover-content-available-height)] w-[28rem] flex-col p-0"
            onInteractOutside={(event) => event.preventDefault()}
            side="bottom"
        >
            <div className="flex items-center justify-between p-3">
                <div>
                    <h4 className="text-sm font-medium">{`Edit ${mcpTool.title || mcpTool.name} Tool`}</h4>

                    <p className="text-xs text-muted-foreground">Configure property values for this tool.</p>
                </div>

                <button className="text-muted-foreground hover:text-foreground" onClick={onClose} type="button">
                    <XIcon className="size-4" />
                </button>
            </div>

            <Form {...form}>
                <form className="flex min-h-0 flex-col" onSubmit={handleSubmit(handleFormSubmit)}>
                    <div className="min-h-0 flex-1 overflow-y-auto p-3">
                        {isLoading ? (
                            <p className="text-sm text-muted-foreground">Loading properties...</p>
                        ) : properties.length > 0 ? (
                            <fieldset className="space-y-4 border-0 p-0">
                                <ClusterElementProvider
                                    value={{
                                        clusterElementName: mcpTool.name,
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

export default McpComponentToolPropertiesPopover;
