import Button from '@/components/Button/Button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import AiAgentEditor from '@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor';
import AiAgentTestingPanel from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel';
import ClusterElementsWorkflowEditor from '@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditor';
import ClusterElementsWorkflowEditorHeader from '@/pages/platform/cluster-element-editor/components/ClusterElementsWorkflowEditorHeader';
import DataStreamEditor from '@/pages/platform/cluster-element-editor/data-stream-editor/DataStreamEditor';
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
import {Suspense, lazy, useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

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
    const [shouldRenderDataPillPanel, setShouldRenderDataPillPanel] = useState(false);
    const [isDataPillPanelVisible, setIsDataPillPanelVisible] = useState(false);

    const {copilotPanelOpen, showAiAgentEditor, showDataStreamEditor, testingPanelOpen} =
        useClusterElementsCanvasDialogStore(
            useShallow((state) => ({
                copilotPanelOpen: state.copilotPanelOpen,
                showAiAgentEditor: state.showAiAgentEditor,
                showDataStreamEditor: state.showDataStreamEditor,
                testingPanelOpen: state.testingPanelOpen,
            }))
        );
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
        isDataStreamClusterRoot,
        isDataStreamSimpleModeAvailable,
    } = useClusterElementsCanvasDialog({
        onOpenChange,
    });

    const ff_4070 = useFeatureFlagsStore()('ff-4070');

    useEffect(() => {
        let outerRafId: number | undefined;
        let innerRafId: number | undefined;
        let timerId: ReturnType<typeof setTimeout> | undefined;

        if (dataPillPanelOpen) {
            setShouldRenderDataPillPanel(true);

            outerRafId = requestAnimationFrame(() => {
                innerRafId = requestAnimationFrame(() => {
                    setIsDataPillPanelVisible(true);
                });
            });
        } else {
            setIsDataPillPanelVisible(false);

            timerId = setTimeout(() => setShouldRenderDataPillPanel(false), 300);
        }

        return () => {
            if (outerRafId !== undefined) {
                cancelAnimationFrame(outerRafId);
            }

            if (innerRafId !== undefined) {
                cancelAnimationFrame(innerRafId);
            }

            if (timerId !== undefined) {
                clearTimeout(timerId);
            }
        };
    }, [dataPillPanelOpen]);

    return (
        <Dialog onOpenChange={handleOpenChange} open={open}>
            <DialogHeader>
                <DialogTitle className="sr-only"></DialogTitle>

                <DialogDescription />
            </DialogHeader>

            <DialogContent className="absolute bottom-4 left-16 top-12 flex h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 flex-col gap-2 bg-surface-main p-0">
                {isDataStreamClusterRoot && showDataStreamEditor ? (
                    <div className="flex size-full min-h-0 overflow-hidden">
                        <DataStreamEditor onClose={handleClose} onToggleEditor={handleToggleEditor} />
                    </div>
                ) : showAiAgentEditor ? (
                    <div className="flex size-full min-h-0 overflow-hidden">
                        <AiAgentEditor
                            className={twMerge(
                                'transition-[margin] duration-300 ease-in-out',
                                copilotPanelOpen && 'mr-[450px]'
                            )}
                            copilotEnabled={ff_4070 && copilotEnabled}
                            onClose={handleClose}
                            onCopilotClick={handleCopilotClick}
                            onToggleEditor={handleToggleEditor}
                            previousComponentDefinitions={previousComponentDefinitions}
                            workflowNodeOutputs={workflowNodeOutputs}
                        />

                        <CopilotPanel
                            className="fixed inset-y-0 right-0 rounded-r-md border-l"
                            onClose={handleCopilotClose}
                            open={copilotPanelOpen}
                        />
                    </div>
                ) : (
                    <>
                        <div
                            className={twMerge(
                                'flex min-h-0 flex-1 flex-col rounded-lg bg-surface-popover-canvas transition-[margin] duration-300 ease-in-out',
                                workflowNodeDetailsPanelOpen && 'mr-[465px]',
                                workflowNodeDetailsPanelOpen && copilotPanelOpen && 'mr-[915px]'
                            )}
                        >
                            {(isAiAgentClusterRoot || isDataStreamClusterRoot) && (
                                <ClusterElementsWorkflowEditorHeader
                                    copilotEnabled={ff_4070 && copilotEnabled}
                                    onCopilotClick={handleCopilotClick}
                                    onTestClick={handleTestClick}
                                    onToggleEditor={handleToggleEditor}
                                    showTestButton={isAiAgentClusterRoot}
                                    showToggleEditor={
                                        isAiAgentClusterRoot ||
                                        (isDataStreamClusterRoot && isDataStreamSimpleModeAvailable)
                                    }
                                    toggleEditorLabel={
                                        isAiAgentClusterRoot
                                            ? 'Switch to AI Agent editor'
                                            : 'Switch to DataStream editor'
                                    }
                                />
                            )}

                            <ClusterElementsWorkflowEditor />
                        </div>

                        <WorkflowNodeDetailsPanel
                            className={twMerge(
                                'fixed inset-y-0 right-0 rounded-l-none transition-[right] duration-300 ease-in-out',
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

                        {shouldRenderDataPillPanel && (
                            <Suspense
                                fallback={
                                    <DataPillPanelSkeleton
                                        className={twMerge(
                                            'fixed inset-y-0 right-[465px] rounded-none transition-[right,transform,opacity] duration-300 ease-in-out',
                                            copilotPanelOpen && 'right-[915px]',
                                            !isDataPillPanelVisible && 'translate-x-8 opacity-0'
                                        )}
                                    />
                                }
                            >
                                <DataPillPanel
                                    className={twMerge(
                                        'fixed inset-y-0 right-[465px] rounded-none transition-[right,transform,opacity] duration-300 ease-in-out',
                                        copilotPanelOpen && 'right-[915px]',
                                        !isDataPillPanelVisible && 'translate-x-8 opacity-0'
                                    )}
                                    previousComponentDefinitions={previousComponentDefinitions}
                                    workflowNodeOutputs={workflowNodeOutputs}
                                />
                            </Suspense>
                        )}

                        {testingPanelOpen && (
                            <div
                                className={twMerge(
                                    'fixed inset-y-0 right-0 z-0 w-[800px] overflow-hidden border-l border-r bg-background transition-[right] duration-300 ease-in-out',
                                    workflowNodeDetailsPanelOpen && !copilotPanelOpen && 'right-[465px]',
                                    copilotPanelOpen && 'right-[450px] z-10'
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
