import Button from '@/components/Button/Button';
import {Form} from '@/components/ui/form';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import Properties from '@/pages/platform/workflow-editor/components/properties/Properties';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {XIcon} from 'lucide-react';
import {ReactNode} from 'react';

import useClusterElementTestPropertiesPopover from './hooks/useClusterElementTestPropertiesPopover';

interface ClusterElementTestPropertiesPopoverProps {
    children: ReactNode;
    currentNode: NodeDataType;
    onOpenChange: (open: boolean) => void;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onSubmit: (inputParameters: Record<string, any>) => void;
    open: boolean;
    properties: PropertyAllType[];
}

const ClusterElementTestPropertiesPopover = ({
    children,
    currentNode,
    onOpenChange,
    onSubmit,
    open,
    properties,
}: ClusterElementTestPropertiesPopoverProps) => {
    const {control, form, formState, handleFormSubmit, handleSubmit, propertiesWithDefaults} =
        useClusterElementTestPropertiesPopover({currentNode, onSubmit, properties});

    return (
        <Popover onOpenChange={onOpenChange} open={open}>
            <PopoverTrigger asChild>{children}</PopoverTrigger>

            <PopoverContent
                align="end"
                alignOffset={-36}
                className="w-[460px] p-0"
                onInteractOutside={(event) => event.preventDefault()}
                side="bottom"
                sideOffset={8}
            >
                <Form {...form}>
                    <form onSubmit={handleSubmit(handleFormSubmit)}>
                        <div className="flex items-center justify-between px-4 py-3">
                            <div>
                                <h3 className="text-sm font-medium">Test Properties</h3>

                                <p className="text-xs text-muted-foreground">
                                    Override property values for testing this tool.
                                </p>
                            </div>

                            <button
                                aria-label="Close"
                                className="text-muted-foreground hover:text-foreground"
                                onClick={() => onOpenChange(false)}
                                type="button"
                            >
                                <XIcon className="size-4" />
                            </button>
                        </div>

                        <div
                            className="max-h-[400px] overflow-y-auto px-4 py-3"
                            onWheel={(event) => event.stopPropagation()}
                        >
                            <fieldset className="space-y-4 border-0 p-0">
                                <Properties
                                    control={control}
                                    controlPath=""
                                    formState={formState}
                                    hideFromAi
                                    properties={propertiesWithDefaults}
                                />
                            </fieldset>
                        </div>

                        <div className="flex justify-end gap-2 px-4 py-3">
                            <Button
                                label="Cancel"
                                onClick={() => onOpenChange(false)}
                                type="button"
                                variant="outline"
                            />

                            <Button label="Test" type="submit" />
                        </div>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

export default ClusterElementTestPropertiesPopover;
