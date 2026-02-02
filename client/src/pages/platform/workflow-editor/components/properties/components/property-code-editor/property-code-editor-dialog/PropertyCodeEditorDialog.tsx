import UnsavedChangesAlertDialog from '@/components/UnsavedChangesAlertDialog';
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import PropertyCodeEditorDialogEditor from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogEditor';
import PropertyCodeEditorDialogExecutionOutput from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogExecutionOutput';
import PropertyCodeEditorDialogRightPanel from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanel';
import PropertyCodeEditorDialogToolbar from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogToolbar';
import {usePropertyCodeEditorDialog} from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/hooks';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {Workflow} from '@/shared/middleware/platform/configuration';

interface PropertyCodeEditorDialogProps {
    language: string;
    onChange: (value: string | undefined) => void;
    onClose?: () => void;
    value?: string;
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditorDialog = (props: PropertyCodeEditorDialogProps) => {
    const {
        copilotPanelOpen,
        currentWorkflowTask,
        handleCopilotClose,
        handleOpenChange,
        handleUnsavedChangesAlertDialogCancel,
        handleUnsavedChangesAlertDialogClose,
        unsavedChangesAlertDialogOpen,
    } = usePropertyCodeEditorDialog(props);

    return (
        <>
            <Dialog onOpenChange={handleOpenChange} open={true}>
                <DialogHeader>
                    <DialogTitle>Edit Script</DialogTitle>

                    <DialogDescription />
                </DialogHeader>

                <DialogContent
                    className="absolute bottom-4 left-16 top-12 flex h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 flex-row gap-0 p-0"
                    key={`dialog-content-${copilotPanelOpen}`}
                >
                    <div className="flex flex-1 flex-col gap-2">
                        <PropertyCodeEditorDialogToolbar
                            language={props.language}
                            onChange={props.onChange}
                            workflowId={props.workflow.id!}
                            workflowNodeName={props.workflowNodeName}
                        />

                        <div className="flex flex-1">
                            <ResizablePanelGroup className="flex-1" direction="vertical">
                                <ResizablePanel defaultSize={75}>
                                    <PropertyCodeEditorDialogEditor language={props.language} />
                                </ResizablePanel>

                                <ResizableHandle className="bg-muted" />

                                <ResizablePanel defaultSize={25}>
                                    <PropertyCodeEditorDialogExecutionOutput />
                                </ResizablePanel>
                            </ResizablePanelGroup>

                            <div className="flex border-l border-l-border/50">
                                <PropertyCodeEditorDialogRightPanel
                                    componentConnections={currentWorkflowTask?.connections || []}
                                    workflow={props.workflow}
                                    workflowNodeName={props.workflowNodeName}
                                />
                            </div>
                        </div>
                    </div>

                    {copilotPanelOpen && (
                        <div className="flex h-full border-l border-l-border/50">
                            <CopilotPanel className="rounded-r-md" onClose={handleCopilotClose} />
                        </div>
                    )}
                </DialogContent>
            </Dialog>

            <UnsavedChangesAlertDialog
                onCancel={handleUnsavedChangesAlertDialogCancel}
                onClose={handleUnsavedChangesAlertDialogClose}
                open={unsavedChangesAlertDialogOpen}
            />
        </>
    );
};

export default PropertyCodeEditorDialog;
