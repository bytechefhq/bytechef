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
import {Input} from '@/components/ui/input';
import {ScrollArea} from '@/components/ui/scroll-area';
import {McpComponent} from '@/shared/middleware/graphql';
import {XIcon} from 'lucide-react';
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
        handleAddRequiredAuthority,
        handleBack,
        handleComponentSelect,
        handleOpenChange,
        handleRemoveRequiredAuthority,
        handleSave,
        requiredAuthorities,
        requiredAuthorityInput,
        selectedComponent,
        selectedConnection,
        selectedTools,
        setRequiredAuthorityInput,
        setSelectedConnection,
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
                        <>
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

                            <fieldset className="mt-4 space-y-2 border-0 p-0 px-1">
                                <label className="text-sm font-medium">Required Authorities</label>

                                <div className="flex gap-2">
                                    <Input
                                        onChange={(event) => setRequiredAuthorityInput(event.target.value)}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter') {
                                                event.preventDefault();
                                                handleAddRequiredAuthority();
                                            }
                                        }}
                                        placeholder="ROLE_SALES"
                                        value={requiredAuthorityInput}
                                    />

                                    <Button
                                        label="Add"
                                        onClick={handleAddRequiredAuthority}
                                        type="button"
                                        variant="outline"
                                    />
                                </div>

                                {requiredAuthorities.length > 0 && (
                                    <div className="flex flex-wrap gap-1">
                                        {requiredAuthorities.map((requiredAuthority) => (
                                            <span
                                                className="inline-flex items-center gap-1 rounded-md bg-secondary px-2 py-0.5 text-xs text-secondary-foreground"
                                                key={requiredAuthority}
                                            >
                                                {requiredAuthority}

                                                <button
                                                    className="ml-1 rounded-full hover:bg-muted"
                                                    onClick={() => handleRemoveRequiredAuthority(requiredAuthority)}
                                                    type="button"
                                                >
                                                    <XIcon className="size-3" />
                                                </button>
                                            </span>
                                        ))}
                                    </div>
                                )}

                                <p className="text-xs text-muted-foreground">
                                    When the server enforces tool authorization, only callers holding one of these
                                    authorities see this component&apos;s tools.
                                </p>
                            </fieldset>
                        </>
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
