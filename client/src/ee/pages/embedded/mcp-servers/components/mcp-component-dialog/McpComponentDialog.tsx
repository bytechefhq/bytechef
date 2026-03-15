import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {ScrollArea} from '@/components/ui/scroll-area';
import {McpComponent} from '@/shared/middleware/graphql';
import {ReactNode} from 'react';

import McpComponentDialogComponentSelectionStep from './McpComponentDialogComponentSelectionStep';
import McpComponentDialogToolSelectionStep from './McpComponentDialogToolSelectionStep';
import useMcpComponentDialog from './hooks/useMcpComponentDialog';

const McpComponentDialog = ({
    mcpComponent,
    mcpServerId,
    onOpenChange,
    open,
    triggerNode,
}: {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    triggerNode?: ReactNode;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}) => {
    const {
        currentStep,
        existingTools,
        handleBack,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        selectedComponent,
        selectedTools,
        setSelectedTools,
    } = useMcpComponentDialog({mcpComponent, mcpServerId, onOpenChange, open});

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent className="max-h-workflow-execution-content-height sm:max-w-output-tab-sample-data-dialog-width">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>
                            {currentStep === 'components'
                                ? 'Select Component'
                                : mcpComponent
                                  ? `Edit Tools for ${selectedComponent?.title || selectedComponent?.name}`
                                  : `Select Tools from ${selectedComponent?.title || selectedComponent?.name}`}
                        </DialogTitle>

                        <DialogDescription>
                            {currentStep === 'components'
                                ? 'Choose a component to add to your MCP server.'
                                : mcpComponent
                                  ? 'Modify the tools enabled for this component.'
                                  : 'Select the tools you want to enable for this component.'}
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <ScrollArea className="max-h-[60vh]">
                    {currentStep === 'components' && (
                        <McpComponentDialogComponentSelectionStep
                            onComponentSelect={handleComponentSelect}
                            open={open ?? true}
                        />
                    )}

                    {currentStep === 'tools' && (
                        <McpComponentDialogToolSelectionStep
                            existingTools={existingTools}
                            onToolsChange={setSelectedTools}
                            selectedComponent={selectedComponent}
                            selectedTools={selectedTools}
                        />
                    )}
                </ScrollArea>

                <DialogFooter>
                    {currentStep === 'tools' && (
                        <div className="flex w-full justify-between">
                            <div className="text-sm text-muted-foreground">
                                {selectedTools.length} tool{selectedTools.length !== 1 ? 's' : ''} selected
                            </div>

                            <div className="flex space-x-2">
                                <DialogClose asChild>
                                    <Button label="Cancel" type="button" variant="outline" />
                                </DialogClose>

                                {currentStep === 'tools' && !mcpComponent && (
                                    <Button label="Back" onClick={handleBack} variant="outline" />
                                )}

                                <Button
                                    disabled={selectedTools.length === 0}
                                    label={mcpComponent ? 'Update' : 'Save'}
                                    onClick={handleSave}
                                />
                            </div>
                        </div>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default McpComponentDialog;
