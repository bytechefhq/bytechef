import {ReactFlowProvider} from '@xyflow/react';

import './WorkflowEditorLayout.css';

import ClusterElementsCanvasDialog from '@/pages/platform/workflow-editor/components/ClusterElementsCanvasDialog';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import useDelayedUnmount from '@/pages/platform/workflow-editor/hooks/useDelayedUnmount';
import useWorkflowEditorLayout from '@/pages/platform/workflow-editor/hooks/useWorkflowEditorLayout';
import useWorkflowIssues from '@/pages/platform/workflow-editor/hooks/useWorkflowIssues';
import useWorkflowIssuesSweep from '@/pages/platform/workflow-editor/hooks/useWorkflowIssuesSweep';
import useWorkflowIssuesValidation from '@/pages/platform/workflow-editor/hooks/useWorkflowIssuesValidation';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import useCopilotStateContributorRegistry from '@/shared/components/copilot/stores/useCopilotStateContributorRegistry';
import {Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ISSUES_SIDEBAR_EXIT_DURATION} from '@/shared/constants';
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
const WorkflowIssuesSidebar = lazy(() => import('./components/WorkflowIssuesSidebar'));
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

    const copilotLayoutShifted = useCopilotLayoutShifted();
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
    const issuesSidebarOpen = useWorkflowIssuesStore((state) => state.issuesSidebarOpen);
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );
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
        handleWorkflowIssuesClick,
        handleWorkflowOutputsClick,
        isWorkflowNodeOutputsPending,
        previousComponentDefinitions,
        taskDispatcherDefinitions,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout(includeComponents);

    useWorkflowIssuesSweep();
    useWorkflowIssuesValidation();

    const issues = useWorkflowIssues();

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();
    const {handleClusterElementsCanvasOpenChange, isMainRootClusterElement} = useWorkflowEditorLayout();

    const queryClient = useQueryClient();
    const {projectId, projectWorkflowId} = useParams();

    const {mounted: rightSidebarMounted, visible: rightSidebarVisible} = useDelayedUnmount(rightSidebarOpen);
    const {mounted: issuesSidebarMounted, visible: issuesSidebarVisible} = useDelayedUnmount(
        issuesSidebarOpen,
        workflowNodeDetailsPanelOpen ? 0 : ISSUES_SIDEBAR_EXIT_DURATION
    );

    useEffect(() => {
        return useCopilotStateContributorRegistry.getState().register(() => {
            const activeWorkflow = useWorkflowDataStore.getState().workflow;
            const activeNode = useWorkflowNodeDetailsPanelStore.getState().currentNode;
            const copilotContext = useCopilotStore.getState().context as {
                workflowExecutionError?: {workflowId?: string};
            };
            const workflowExecutionError = copilotContext.workflowExecutionError;

            return {
                currentSelectedNode: activeNode?.name,
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

            useWorkflowNodeDetailsPanelStore.getState().clearPendingSaveNodeNames();
            useWorkflowIssuesStore.getState().reset();
        };
    }, []);

    return (
        <ReactFlowProvider>
            <div
                className={twMerge(
                    'relative mx-3 mb-3 flex w-full overflow-hidden rounded-lg border border-stroke-neutral-secondary',
                    leftSidebarOpen && 'ml-0',
                    copilotLayoutShifted && 'mr-0'
                )}
            >
                <div className="absolute top-2 left-2 z-10 flex flex-col gap-2">
                    <SubflowBanner />
                </div>

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

                {issuesSidebarMounted && (
                    <Suspense>
                        <WorkflowIssuesSidebar visible={issuesSidebarVisible} />
                    </Suspense>
                )}

                {componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense
                        fallback={
                            <WorkflowRightSidebarSkeleton itemCount={!showCopilot && !showWorkflowInputs ? 3 : 5} />
                        }
                    >
                        <WorkflowRightSidebar
                            copilotPanelOpen={copilotPanelOpen}
                            issueCount={issues.length}
                            issueSeverity={issues[0]?.severity}
                            issuesSidebarOpen={issuesSidebarOpen}
                            onComponentsAndFlowControlsClick={handleComponentsAndFlowControlsClick}
                            onCopilotClick={handleCopilotClick}
                            onWorkflowCodeEditorClick={handleWorkflowCodeEditorClick}
                            onWorkflowInputsClick={handleWorkflowInputsClick}
                            onWorkflowIssuesClick={handleWorkflowIssuesClick}
                            onWorkflowOutputsClick={handleWorkflowOutputsClick}
                            rightSidebarOpen={rightSidebarOpen}
                            showCopilot={showCopilot}
                            showWorkflowInputs={showWorkflowInputs}
                        />
                    </Suspense>
                )}
            </div>

            {currentNode?.type && !isMainRootClusterElement && !clusterElementsCanvasOpen && (
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

            {currentNode?.type && !isMainRootClusterElement && !clusterElementsCanvasOpen && dataPillPanelOpen && (
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
