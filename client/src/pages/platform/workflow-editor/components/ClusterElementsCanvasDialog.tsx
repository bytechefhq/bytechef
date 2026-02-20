import Button from '@/components/Button/Button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import AiAgentEditor from '@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor';
import AiAgentTestingPanel from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel';
import ClusterElementsWorkflowEditor from '@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor';
import ClusterElementsWorkflowEditorHeader from '@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditorHeader';
import {DataPillPanelSkeleton} from '@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import useClusterElementsCanvasDialog from '@/pages/platform/workflow-editor/components/hooks/useClusterElementsCanvasDialog';
import {useClusterElementsCanvasDialogStore} from '@/pages/platform/workflow-editor/components/stores/useClusterElementsCanvasDialogStore';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {XIcon} from 'lucide-react';
import {Suspense, lazy} from 'react';
import {twMerge} from 'tailwind-merge';

const DataPillPanel = lazy(() => import('./datapills/DataPillPanel'));

interface ClusterElementsCanvasDialogProps {
    invalidateWorkflowQueries: () => void;
    onOpenChange: (open: boolean) => void;
    open: boolean;
    previousComponentDefinitions: ComponentDefinitionBasic[];
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
}

const ClusterElementsCanvasDialog = ({
    invalidateWorkflowQueries,
    onOpenChange,
    open,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
}: ClusterElementsCanvasDialogProps) => {
    const copilotPanelOpen = useClusterElementsCanvasDialogStore((state) => state.copilotPanelOpen);
    const showAiAgentEditor = useClusterElementsCanvasDialogStore((state) => state.showAiAgentEditor);
    const testingPanelOpen = useClusterElementsCanvasDialogStore((state) => state.testingPanelOpen);
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );

    const {
        copilotEnabled,
        handleClose,
        handleCloseTestingPanel,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        handleTestClick,
        handleToggleEditor,
        isAiAgentClusterRoot,
    } = useClusterElementsCanvasDialog({
        onOpenChange,
    });

    const ff_4070 = useFeatureFlagsStore()('ff-4070');

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogHeader>
                <DialogTitle className="sr-only"></DialogTitle>

                <DialogDescription />
            </DialogHeader>

            <DialogContent className="absolute bottom-4 left-16 top-12 h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 gap-2 bg-surface-main p-0">
                {showAiAgentEditor ? (
                    <div className="flex size-full min-h-0 overflow-hidden">
                        <AiAgentEditor
                            className={twMerge(
                                copilotPanelOpen && 'mr-[450px]',
                                dataPillPanelOpen && 'mr-[400px]',
                                dataPillPanelOpen && copilotPanelOpen && 'mr-[850px]'
                            )}
                            copilotEnabled={ff_4070 && copilotEnabled}
                            onClose={handleClose}
                            onCopilotClick={handleCopilotClick}
                            onToggleEditor={handleToggleEditor}
                            previousComponentDefinitions={previousComponentDefinitions}
                            workflowNodeOutputs={workflowNodeOutputs}
                        />

                        {dataPillPanelOpen && (
                            <Suspense fallback={<DataPillPanelSkeleton />}>
                                <DataPillPanel
                                    className={twMerge(
                                        'fixed inset-y-0 right-0 rounded-none',
                                        copilotPanelOpen && 'right-[450px]'
                                    )}
                                    previousComponentDefinitions={previousComponentDefinitions}
                                    workflowNodeOutputs={workflowNodeOutputs}
                                />
                            </Suspense>
                        )}

                        <CopilotPanel
                            className="fixed inset-y-0 right-0 rounded-r-md border-l"
                            onClose={handleCopilotClose}
                            open={copilotPanelOpen}
                        />
                    </div>
                ) : (
                    <>
                        <div className="flex size-full flex-col rounded-lg bg-surface-popover-canvas">
                            {isAiAgentClusterRoot && (
                                <ClusterElementsWorkflowEditorHeader
                                    className={twMerge(
                                        workflowNodeDetailsPanelOpen && 'pr-[470px]',
                                        workflowNodeDetailsPanelOpen && copilotPanelOpen && 'pr-[920px]'
                                    )}
                                    copilotEnabled={ff_4070 && copilotEnabled}
                                    onCopilotClick={handleCopilotClick}
                                    onTestClick={handleTestClick}
                                    onToggleEditor={handleToggleEditor}
                                />
                            )}

                            <ClusterElementsWorkflowEditor />
                        </div>

                        <WorkflowNodeDetailsPanel
                            className={twMerge(
                                'fixed inset-y-0 right-0 rounded-l-none',
                                copilotPanelOpen && 'right-[450px] rounded-r-none border-r-0'
                            )}
                            closeButton={
                                <DialogClose asChild>
                                    <Button icon={<XIcon />} size="icon" title="Close the canvas" variant="ghost" />
                                </DialogClose>
                            }
                            invalidateWorkflowQueries={invalidateWorkflowQueries}
                            previousComponentDefinitions={previousComponentDefinitions}
                            updateWorkflowMutation={updateWorkflowMutation}
                            workflowNodeOutputs={workflowNodeOutputs}
                        />

                        {dataPillPanelOpen && (
                            <Suspense fallback={<DataPillPanelSkeleton />}>
                                <DataPillPanel
                                    className={twMerge(
                                        'fixed inset-y-0 right-[465px] rounded-none',
                                        testingPanelOpen && 'right-[1265px]',
                                        copilotPanelOpen && 'right-[915px]',
                                        testingPanelOpen && copilotPanelOpen && 'right-[1250px]'
                                    )}
                                    previousComponentDefinitions={previousComponentDefinitions}
                                    workflowNodeOutputs={workflowNodeOutputs}
                                />
                            </Suspense>
                        )}

                        {testingPanelOpen && (
                            <div
                                className={twMerge(
                                    'fixed inset-y-0 right-0 z-10 w-[800px] overflow-hidden border-l border-r bg-background',
                                    workflowNodeDetailsPanelOpen && !copilotPanelOpen && 'right-[465px]',
                                    copilotPanelOpen && 'right-[450px]'
                                )}
                            >
                                <AiAgentTestingPanel
                                    contentClassName="rounded-none"
                                    copilotEnabled={ff_4070 && copilotEnabled}
                                    headerClassName="px-4 pt-4"
                                    onClose={handleCloseTestingPanel}
                                    onCopilotClick={handleCopilotClick}
                                />
                            </div>
                        )}

                        <CopilotPanel
                            className="fixed inset-y-0 right-0 rounded-r-md border-l"
                            onClose={handleCopilotClose}
                            open={copilotPanelOpen}
                        />
                    </>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ClusterElementsCanvasDialog;
