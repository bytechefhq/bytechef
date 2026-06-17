import {ReactFlowProvider} from '@xyflow/react';

import './WorkflowEditorLayout.css';

import ClusterElementsCanvasDialog from '@/pages/platform/workflow-editor/components/ClusterElementsCanvasDialog';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import useWorkflowEditorLayout from '@/pages/platform/workflow-editor/hooks/useWorkflowEditorLayout';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {Suspense, lazy, useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import SubflowBanner from './components/SubflowBanner';
import WorkflowCodeEditorSheet from './components/WorkflowCodeEditorSheet';
import {
    DataPillPanelSkeleton,
    WorkflowNodesSidebarSkeleton,
    WorkflowRightSidebarSkeleton,
} from './components/WorkflowEditorSkeletons';
import WorkflowOutputsSheet from './components/WorkflowOutputsSheet';
import WorkflowInputsSheet from './components/workflow-inputs/WorkflowInputsSheet';
import useDataPillPanelStore from './stores/useDataPillPanelStore';
import useWorkflowDataStore from './stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from './stores/useWorkflowNodeDetailsPanelStore';
import {clearAllWorkflowMutations} from './utils/workflowMutationGuard';

const DataPillPanel = lazy(() => import('./components/datapills/DataPillPanel'));
const WorkflowEditor = lazy(() => import('./components/WorkflowEditor'));
const WorkflowRightSidebar = lazy(() => import('./components/WorkflowRightSidebar'));
const WorkflowNodesSidebar = lazy(() => import('./components/WorkflowNodesSidebar'));

interface WorkflowEditorLayoutProps {
    enableUndoRedo?: boolean;
    includeComponents?: string[];
    leftSidebarOpen?: boolean;
    onEditSubflowClick?: (workflowUuid: string) => void;
    runDisabled: boolean;
    showCopilot?: boolean;
    showWorkflowInputs: boolean;
    workflowReferenceId?: number | string;
}

const WorkflowEditorLayout = ({
    enableUndoRedo,
    includeComponents,
    leftSidebarOpen,
    onEditSubflowClick,
    runDisabled,
    showCopilot = true,
    showWorkflowInputs,
    workflowReferenceId,
}: WorkflowEditorLayoutProps) => {
    const [clusterDialogMounted, setClusterDialogMounted] = useState(false);
    const [rightSidebarMounted, setRightSidebarMounted] = useState(false);
    const [rightSidebarVisible, setRightSidebarVisible] = useState(false);

    const copilotLayoutShifted = useCopilotLayoutShifted();
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);
    const {
        clusterElementsCanvasOpen,
        setShowWorkflowCodeEditorSheet,
        setShowWorkflowInputsSheet,
        setShowWorkflowOutputsSheet,
        showWorkflowCodeEditorSheet,
        showWorkflowInputsSheet,
        showWorkflowOutputsSheet,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            setShowWorkflowCodeEditorSheet: state.setShowWorkflowCodeEditorSheet,
            setShowWorkflowInputsSheet: state.setShowWorkflowInputsSheet,
            setShowWorkflowOutputsSheet: state.setShowWorkflowOutputsSheet,
            showWorkflowCodeEditorSheet: state.showWorkflowCodeEditorSheet,
            showWorkflowInputsSheet: state.showWorkflowInputsSheet,
            showWorkflowOutputsSheet: state.showWorkflowOutputsSheet,
        }))
    );
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);

    const {
        componentDefinitions,
        filteredWorkflowNodeOutputs,
        handleComponentsAndFlowControlsClick,
        handleCopilotClick,
        handleWorkflowCodeEditorClick,
        handleWorkflowInputsClick,
        handleWorkflowOutputsClick,
        isWorkflowNodeOutputsPending,
        previousComponentDefinitions,
        taskDispatcherDefinitions,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout(includeComponents);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();
    const {handleClusterElementsCanvasOpenChange, isMainRootClusterElement} = useWorkflowEditorLayout();

    const queryClient = useQueryClient();
    const {projectId, projectWorkflowId} = useParams();

    useEffect(() => {
        return useCopilotStateContributorRegistry.getState().register(() => {
            const activeWorkflow = useWorkflowDataStore.getState().workflow;
            const activeComponent = useWorkflowNodeDetailsPanelStore.getState().currentComponent;
            const copilotContext = useCopilotStore.getState().context as {
                workflowExecutionError?: {workflowId?: string};
            };
            const workflowExecutionError = copilotContext.workflowExecutionError;

            return {
                currentSelectedNode: activeComponent?.name,
                workflowId: activeWorkflow.id,
                ...(activeWorkflow.id === workflowExecutionError?.workflowId ? {workflowExecutionError} : {}),
            };
        });
    }, []);

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR, () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
        });
    }, [projectId, projectWorkflowId, queryClient]);

    useEffect(() => {
        let outerRafId: number | undefined;
        let innerRafId: number | undefined;
        let timerId: ReturnType<typeof setTimeout> | undefined;

        if (rightSidebarOpen) {
            setRightSidebarMounted(true);

            outerRafId = requestAnimationFrame(() => {
                innerRafId = requestAnimationFrame(() => {
                    setRightSidebarVisible(true);
                });
            });
        } else {
            setRightSidebarVisible(false);

            timerId = setTimeout(() => setRightSidebarMounted(false), 300);
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
    }, [rightSidebarOpen]);

    useEffect(() => {
        if (clusterElementsCanvasOpen) {
            setClusterDialogMounted(true);
        } else {
            const timerId = setTimeout(() => setClusterDialogMounted(false), 300);

            return () => clearTimeout(timerId);
        }
    }, [clusterElementsCanvasOpen]);

    useEffect(() => {
        return () => {
            clearAllWorkflowMutations();
        };
    }, []);

    return (
        <ReactFlowProvider>
            <div
                className={twMerge(
                    'relative mx-3 mb-3 flex w-full',
                    leftSidebarOpen && 'ml-0',
                    copilotLayoutShifted && 'mr-0'
                )}
            >
                <SubflowBanner />

                {componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense>
                        <WorkflowEditor
                            componentDefinitions={componentDefinitions}
                            enableUndoRedo={enableUndoRedo}
                            leftSidebarOpen={leftSidebarOpen}
                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                        />
                    </Suspense>
                )}

                {rightSidebarMounted && componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense fallback={<WorkflowNodesSidebarSkeleton />}>
                        <WorkflowNodesSidebar
                            data={{
                                componentDefinitions,
                                taskDispatcherDefinitions,
                            }}
                            visible={rightSidebarVisible}
                        />
                    </Suspense>
                )}

                {componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense
                        fallback={
                            <WorkflowRightSidebarSkeleton itemCount={!showCopilot && !showWorkflowInputs ? 2 : 4} />
                        }
                    >
                        <WorkflowRightSidebar
                            copilotPanelOpen={copilotPanelOpen}
                            onComponentsAndFlowControlsClick={handleComponentsAndFlowControlsClick}
                            onCopilotClick={handleCopilotClick}
                            onWorkflowCodeEditorClick={handleWorkflowCodeEditorClick}
                            onWorkflowInputsClick={handleWorkflowInputsClick}
                            onWorkflowOutputsClick={handleWorkflowOutputsClick}
                            rightSidebarOpen={rightSidebarOpen}
                            showCopilot={showCopilot}
                            showWorkflowInputs={showWorkflowInputs}
                        />
                    </Suspense>
                )}
            </div>

            {currentComponent && !isMainRootClusterElement && !clusterElementsCanvasOpen && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation!}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            {clusterDialogMounted && (
                <ClusterElementsCanvasDialog
                    onOpenChange={handleClusterElementsCanvasOpenChange}
                    open={clusterElementsCanvasOpen}
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation!}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    workflowReferenceId={workflowReferenceId}
                />
            )}

            {workflow.id && <WorkflowTestChatPanel />}

            {currentComponent && !isMainRootClusterElement && !clusterElementsCanvasOpen && dataPillPanelOpen && (
                <Suspense fallback={<DataPillPanelSkeleton />}>
                    <DataPillPanel
                        loading={isWorkflowNodeOutputsPending}
                        previousComponentDefinitions={previousComponentDefinitions}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    />
                </Suspense>
            )}

            {showWorkflowInputsSheet && (
                <WorkflowInputsSheet
                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                    onSheetOpenChange={setShowWorkflowInputsSheet}
                    sheetOpen={showWorkflowInputsSheet}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            )}

            {showWorkflowOutputsSheet && (
                <WorkflowOutputsSheet
                    onSheetOpenChange={setShowWorkflowOutputsSheet}
                    sheetOpen={showWorkflowOutputsSheet}
                    workflow={workflow}
                />
            )}

            {showWorkflowCodeEditorSheet && (
                <WorkflowCodeEditorSheet
                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                    onEditSubflowClick={onEditSubflowClick}
                    onSheetOpenClose={setShowWorkflowCodeEditorSheet}
                    runDisabled={runDisabled}
                    sheetOpen={showWorkflowCodeEditorSheet}
                    testConfigurationDisabled={testConfigurationDisabled}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            )}
        </ReactFlowProvider>
    );
};

export default WorkflowEditorLayout;
