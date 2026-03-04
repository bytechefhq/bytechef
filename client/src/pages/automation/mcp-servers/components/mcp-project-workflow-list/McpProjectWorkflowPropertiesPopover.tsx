import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {PopoverContent} from '@/components/ui/popover';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {McpProjectWorkflow} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';

import useMcpProjectWorkflowPropertiesPopover from './hooks/useMcpProjectWorkflowPropertiesPopover';

interface McpProjectWorkflowPropertiesPopoverProps {
    mcpProjectWorkflow: McpProjectWorkflow;
    onClose: () => void;
}

const McpProjectWorkflowPropertiesPopover = ({
    mcpProjectWorkflow,
    onClose,
}: McpProjectWorkflowPropertiesPopoverProps) => {
    const {control, form, formState, handleFormSubmit, handleSubmit, isLoading, properties} =
        useMcpProjectWorkflowPropertiesPopover(mcpProjectWorkflow, onClose);

    return (
        <PopoverContent
            align="start"
            className="flex max-h-[var(--radix-popover-content-available-height)] w-[28rem] flex-col p-0"
            onInteractOutside={(event) => event.preventDefault()}
            side="bottom"
        >
            <div className="flex items-center justify-between p-3">
                <div>
                    <h4 className="text-sm font-medium">{`Edit ${mcpProjectWorkflow.workflow?.label || 'Workflow'} Tool`}</h4>

                    <p className="text-xs text-muted-foreground">Configure property values for this workflow tool.</p>
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
                                <Properties
                                    control={control}
                                    controlPath=""
                                    formState={formState}
                                    properties={properties}
                                    toolsMode
                                />
                            </fieldset>
                        ) : (
                            <p className="text-sm text-muted-foreground">
                                No configurable properties for this workflow tool.
                            </p>
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

export default McpProjectWorkflowPropertiesPopover;
