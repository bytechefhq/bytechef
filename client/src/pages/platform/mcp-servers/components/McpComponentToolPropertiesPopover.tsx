import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {PopoverContent} from '@/components/ui/popover';
import McpComponentToolForm from '@/pages/platform/mcp-servers/components/McpComponentToolForm';
import useMcpComponentToolPropertiesPopover from '@/pages/platform/mcp-servers/hooks/useMcpComponentToolPropertiesPopover';
import {McpTool} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';

interface McpComponentToolPropertiesPopoverProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    embedded?: boolean;
    mcpTool: McpTool;
    onClose: () => void;
}

const McpComponentToolPropertiesPopover = ({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    embedded,
    mcpTool,
    onClose,
}: McpComponentToolPropertiesPopoverProps) => {
    const {form, handleFormSubmit, handleSubmit, isLoading, properties} = useMcpComponentToolPropertiesPopover(
        componentName,
        componentVersion,
        mcpTool,
        onClose,
        embedded
    );

    let propertiesContent;

    if (isLoading) {
        propertiesContent = <p className="text-sm text-muted-foreground">Loading properties...</p>;
    } else if (properties.length === 0) {
        propertiesContent = <p className="text-sm text-muted-foreground">No configurable properties for this tool.</p>;
    } else {
        propertiesContent = (
            <McpComponentToolForm
                componentName={componentName}
                componentVersion={componentVersion}
                connectionId={connectionId}
                connectionRequired={connectionRequired}
                form={form}
                properties={properties}
                toolName={mcpTool.name}
            />
        );
    }

    return (
        <PopoverContent
            align="end"
            className="flex max-h-(--radix-popover-content-available-height) w-md flex-col p-0"
            onInteractOutside={(event) => event.preventDefault()}
            side="bottom"
        >
            <div className="flex items-center justify-between p-3">
                <div>
                    <h4 className="text-sm font-medium">{`Edit ${mcpTool.title || mcpTool.name} Tool`}</h4>

                    <p className="text-xs text-muted-foreground">Configure property values for this tool.</p>
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
                <form className="flex min-h-0 flex-col" noValidate onSubmit={handleSubmit(handleFormSubmit)}>
                    <div className="min-h-0 flex-1 overflow-y-auto p-3">{propertiesContent}</div>

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
