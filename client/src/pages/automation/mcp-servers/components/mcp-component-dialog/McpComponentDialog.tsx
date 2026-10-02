import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {ScrollArea} from '@/components/ui/scroll-area';
import {McpComponent} from '@/shared/middleware/graphql';
import {ReactNode} from 'react';

import McpComponentDialogComponentSelectionStep from './McpComponentDialogComponentSelectionStep';
import McpComponentDialogToolSelectionStep from './McpComponentDialogToolSelectionStep';
import useMcpComponentDialog from './hooks/useMcpComponentDialog';

interface McpComponentDialogProps {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    triggerNode?: ReactNode;
    open?: boolean;
    onOpenChange?: (open: boolean) => void;
}

const McpComponentDialog = ({mcpComponent, mcpServerId, onOpenChange, open, triggerNode}: McpComponentDialogProps) => {
    const {
        currentStep,
        existingTools,
        handleBack,
        handleComponentSelect,
        handleOpenChange,
        handleSave,
        selectedComponent,
        selectedConnection,
        selectedTools,
        setSelectedConnection,
        setSelectedTools,
    } = useMcpComponentDialog({mcpComponent, mcpServerId, onOpenChange, open});

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent size="lg">
                <DialogMain>
                    <DialogHeader
                        description={
                            currentStep === 'components'
                                ? 'Choose a component to add to your MCP server.'
                                : mcpComponent
                                  ? 'Modify the tools enabled for this component.'
                                  : 'Select the tools you want to enable for this component.'
                        }
                        title={
                            currentStep === 'components'
                                ? 'Select Component'
                                : mcpComponent
                                  ? `Edit Tools for ${selectedComponent?.title || selectedComponent?.name}`
                                  : `Select Tools from ${selectedComponent?.title || selectedComponent?.name}`
                        }
                    />

                    <DialogBody>
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
                                    mcpComponent={mcpComponent}
                                    onConnectionChange={setSelectedConnection}
                                    onToolsChange={setSelectedTools}
                                    open={open ?? true}
                                    selectedComponent={selectedComponent}
                                    selectedConnection={selectedConnection}
                                    selectedTools={selectedTools}
                                />
                            )}
                        </ScrollArea>
                    </DialogBody>

                    <DialogFooter>
                        {currentStep === 'tools' && (
                            <div className="flex w-full justify-between">
                                <div className="text-sm text-muted-foreground">
                                    {selectedTools.length} tool{selectedTools.length !== 1 ? 's' : ''} selected
                                </div>

                                <div className="flex space-x-2">
                                    <DialogCancelButton />

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
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default McpComponentDialog;
