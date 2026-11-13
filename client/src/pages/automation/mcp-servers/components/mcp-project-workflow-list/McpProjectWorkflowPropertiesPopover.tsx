import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {PopoverContent} from '@/components/ui/popover';
import McpWorkflowToolForm from '@/pages/platform/mcp-servers/components/McpWorkflowToolForm';
import {XIcon} from 'lucide-react';

import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';
import useMcpProjectWorkflowPropertiesPopover from './hooks/useMcpProjectWorkflowPropertiesPopover';

interface McpProjectWorkflowPropertiesPopoverProps {
    mcpProjectWorkflow: McpProjectWorkflowItemType;
    onClose: () => void;
}

const McpProjectWorkflowPropertiesPopover = ({
    mcpProjectWorkflow,
    onClose,
}: McpProjectWorkflowPropertiesPopoverProps) => {
    const {form, handleFormSubmit, handleSubmit, isLoading, properties} = useMcpProjectWorkflowPropertiesPopover(
        mcpProjectWorkflow,
        onClose
    );

    return (
        <PopoverContent
            align="end"
            className="flex max-h-(--radix-popover-content-available-height) w-md flex-col p-0"
            onInteractOutside={(event) => event.preventDefault()}
            side="bottom"
        >
            <div className="flex items-center justify-between p-3">
                <div>
                    <h4 className="text-sm font-medium">{`Edit ${mcpProjectWorkflow.workflow?.label || 'Workflow'} Tool`}</h4>

                    <p className="text-xs text-muted-foreground">Configure property values for this workflow tool.</p>
                </div>

                <Button
                    aria-label="Close"
                    className="text-muted-foreground hover:text-foreground"
                    icon={<XIcon className="size-4" />}
                    onClick={onClose}
                    size="iconXs"
                    variant="ghost"
                />
            </div>

            <Form {...form}>
                <form className="flex min-h-0 flex-col" noValidate onSubmit={handleSubmit(handleFormSubmit)}>
                    <div className="min-h-0 flex-1 overflow-y-auto p-3">
                        {isLoading ? (
                            <p className="text-sm text-muted-foreground">Loading properties...</p>
                        ) : properties.length > 0 ? (
                            <McpWorkflowToolForm form={form} properties={properties} />
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
