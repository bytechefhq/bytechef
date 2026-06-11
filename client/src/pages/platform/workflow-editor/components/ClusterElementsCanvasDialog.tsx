import Button from '@/components/Button/Button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import AiAgentEditor from '@/pages/platform/cluster-element-editor/ai-agent-editor/AiAgentEditor';
import AiAgentTestingPanel from '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/AiAgentTestingPanel';
import AiAgentEvals from '@/pages/platform/cluster-element-editor/ai-agent-evals/AiAgentEvals';
import useAiAgentEvals from '@/pages/platform/cluster-element-editor/ai-agent-evals/hooks/useAiAgentEvals';
import {useAiAgentEvalsStore} from '@/pages/platform/cluster-element-editor/ai-agent-evals/stores/useAiAgentEvalsStore';
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
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {ComponentDefinitionBasic, WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {XIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const DataPillPanel = lazy(() => import('./datapills/DataPillPanel'));

interface ClusterElementsCanvasDialogProps {
    onOpenChange: (open: boolean) => void;
    open: boolean;
    previousComponentDefinitions: ComponentDefinitionBasic[];
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflowNodeOutputs: WorkflowNodeOutput[];
    workflowReferenceId?: number | string;
}

const ClusterElementsCanvasDialog = ({
    onOpenChange,
    open,
    previousComponentDefinitions,
    updateWorkflowMutation,
    workflowNodeOutputs,
    workflowReferenceId,
}: ClusterElementsCanvasDialogProps) => {
    const [shouldRenderDataPillPanel, setShouldRenderDataPillPanel] = useState(false);
    const [isDataPillPanelVisible, setIsDataPillPanelVisible] = useState(false);

    const {evalsPanelOpen, setEvalsPanelOpen} = useAiAgentEvalsStore();
    const edition = useApplicationInfoStore((state) => state.application?.edition);
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
    const ff_4070 = useFeatureFlagsStore()('ff-4070');
    const ff_4553 = useFeatureFlagsStore()('ff-4553');

    const {handleClose: handleEvalsClose} = useAiAgentEvals();
    const {
        copilotEnabled,
        handleClose,
        handleCloseTestingPanel,
        handleCopilotClick,
        handleCopilotClose,
        handleOpenChange,
        handlePointerDownOutside,
        handleTestClick,
        handleToggleEditor,
        isAiAgentClusterRoot,
        isDataStreamClusterRoot,
        isDataStreamSimpleModeAvailable,
    } = useClusterElementsCanvasDialog({
        onOpenChange,
        workflowReferenceId,
    });

    const queryClient = useQueryClient();
    const {projectId, projectWorkflowId} = useParams();

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.CLUSTER_ELEMENT, () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
        });
    }, [projectId, projectWorkflowId, queryClient]);

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

            <DialogContent
                className="absolute top-12 bottom-4 left-16 flex h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 flex-col gap-2 overflow-hidden bg-surface-main p-0 duration-300 data-[state=closed]:slide-out-to-left-0 data-[state=closed]:slide-out-to-top-0 data-[state=closed]:zoom-out-95 data-[state=open]:slide-in-from-left-0 data-[state=open]:slide-in-from-top-0 data-[state=open]:zoom-in-95"
                onPointerDownOutside={handlePointerDownOutside}
            >
                {isDataStreamClusterRoot && showDataStreamEditor ? (
                    <div className="flex size-full min-h-0 overflow-hidden">
                        <DataStreamEditor
                            className={twMerge(
                                'transition-[margin] duration-300 ease-in-out',
                                copilotPanelOpen && 'mr-[450px]'
                            )}
                            copilotEnabled={ff_4070 && copilotEnabled}
                            onClose={handleClose}
                            onCopilotClick={handleCopilotClick}
                            onToggleEditor={isDataStreamSimpleModeAvailable ? handleToggleEditor : undefined}
                        />

                        <CopilotPanel
                            className="absolute inset-y-0 right-0 rounded-r-md border-l border-l-border/50"
                            headerClassName="py-4"
                            onClose={handleCopilotClose}
                            open={copilotPanelOpen}
                        />
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
                            className="absolute inset-y-0 right-0 rounded-r-md border-l border-l-border/50"
                            headerClassName="py-4"
                            onClose={handleCopilotClose}
                            open={copilotPanelOpen}
                        />
                    </div>
                ) : (
                    <>
                        <div
                            className={twMerge(
                                'relative min-h-0 flex-1 rounded-lg bg-surface-popover-canvas transition-[margin] duration-300 ease-in-out',
                                copilotPanelOpen && !workflowNodeDetailsPanelOpen && 'mr-[450px]',
                                workflowNodeDetailsPanelOpen && !copilotPanelOpen && 'mr-[465px]',
                                workflowNodeDetailsPanelOpen && copilotPanelOpen && 'mr-[915px]'
                            )}
                        >
                            <div className="absolute inset-0">
                                <ClusterElementsWorkflowEditor />
                            </div>

                            <ClusterElementsWorkflowEditorHeader
                                copilotEnabled={ff_4070 && copilotEnabled}
                                onCopilotClick={handleCopilotClick}
                                onEvalsClick={
                                    ff_4553 && isAiAgentClusterRoot ? () => setEvalsPanelOpen(true) : undefined
                                }
                                onTestClick={handleTestClick}
                                onToggleEditor={handleToggleEditor}
                                showSkills={edition === EditionType.EE && isAiAgentClusterRoot}
                                showTestButton={isAiAgentClusterRoot}
                                showToggleEditor={
                                    isAiAgentClusterRoot || (isDataStreamClusterRoot && isDataStreamSimpleModeAvailable)
                                }
                                toggleEditorLabel={
                                    isAiAgentClusterRoot ? 'Switch to AI Agent editor' : 'Switch to DataStream editor'
                                }
                            />
                        </div>

                        <WorkflowNodeDetailsPanel
                            className={twMerge(
                                'absolute inset-y-0 right-0 rounded-l-none transition-[right] duration-300 ease-in-out',
                                copilotPanelOpen && 'right-[450px] rounded-r-none border-r-0'
                            )}
                            closeButton={
                                <DialogClose asChild>
                                    <Button icon={<XIcon />} size="icon" title="Close the canvas" variant="ghost" />
                                </DialogClose>
                            }
                            previousComponentDefinitions={previousComponentDefinitions}
                            updateWorkflowMutation={updateWorkflowMutation}
                            workflowNodeOutputs={workflowNodeOutputs}
                        />

                        {shouldRenderDataPillPanel && (
                            <Suspense
                                fallback={
                                    <DataPillPanelSkeleton
                                        className={twMerge(
                                            'absolute inset-y-0 right-[465px] rounded-none transition-[right,transform,opacity] duration-300 ease-in-out',
                                            copilotPanelOpen && 'right-[915px]',
                                            !isDataPillPanelVisible && 'translate-x-8 opacity-0'
                                        )}
                                    />
                                }
                            >
                                <DataPillPanel
                                    className={twMerge(
                                        'absolute inset-y-0 right-[465px] rounded-none transition-[right,transform,opacity] duration-300 ease-in-out',
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
                                    'absolute inset-y-0 right-0 z-10 w-[800px] overflow-hidden border-r border-l bg-background transition-[right] duration-300 ease-in-out',
                                    workflowNodeDetailsPanelOpen && !copilotPanelOpen && 'right-[465px]',
                                    copilotPanelOpen && !workflowNodeDetailsPanelOpen && 'right-[450px]',
                                    copilotPanelOpen && workflowNodeDetailsPanelOpen && 'right-[915px]'
                                )}
                            >
                                <AiAgentTestingPanel
                                    contentClassName="rounded-none"
                                    headerClassName="px-4 pt-4"
                                    onClose={handleCloseTestingPanel}
                                />
                            </div>
                        )}

                        {evalsPanelOpen && (
                            <div className="absolute inset-0 z-20 flex flex-col overflow-y-auto rounded-lg bg-white">
                                <div className="flex items-center justify-between border-b border-b-border/50 p-4">
                                    <div className="text-lg font-semibold">Evals</div>

                                    <Button
                                        icon={<XIcon />}
                                        onClick={handleEvalsClose}
                                        size="icon"
                                        title="Close evals"
                                        variant="ghost"
                                    />
                                </div>

                                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                                    <AiAgentEvals />
                                </div>
                            </div>
                        )}

                        <CopilotPanel
                            className="absolute inset-y-0 right-0 rounded-r-md border-l"
                            headerClassName="py-6"
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
