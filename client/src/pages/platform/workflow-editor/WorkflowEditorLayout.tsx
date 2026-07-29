import {ReactFlowProvider} from '@xyflow/react';

import './WorkflowEditorLayout.css';

import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
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
import {WorkflowKeys} from '@/shared/queries/automation/workflows.queries';
import {useQueryClient} from '@tanstack/react-query';
import {Suspense, lazy, useEffect, useMemo, useState} from 'react';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import ErrorsBanner from './components/ErrorsBanner';
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
import useWorkflowTestChatStore from './stores/useWorkflowTestChatStore';
import {clearAllWorkflowMutations} from './utils/workflowMutationGuard';

const ClusterElementsCanvasDialog = lazy(
    () => import('@/pages/platform/workflow-editor/components/ClusterElementsCanvasDialog')
);
const IntegrationCodeWorkflowDetail = lazy(
    () => import('@/pages/platform/code-workflow/IntegrationCodeWorkflowDetail')
);
const ProjectCodeWorkflowDetail = lazy(() => import('@/pages/platform/code-workflow/ProjectCodeWorkflowDetail'));
const DataPillPanel = lazy(() => import('./components/datapills/DataPillPanel'));
const WorkflowEditor = lazy(() => import('./components/WorkflowEditor'));
const WorkflowTestChatPanel = lazy(
    () => import('@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel')
);
const WorkflowRightSidebar = lazy(() => import('./components/WorkflowRightSidebar'));
const WorkflowNodesSidebar = lazy(() => import('./components/WorkflowNodesSidebar'));

// Polyglot code-workflow languages that get the Monaco source editor instead of the React Flow
// canvas. Java-backed code workflows keep the visual editor since they still compile to a task graph.
const POLYGLOT_CODE_WORKFLOW_LANGUAGES = ['JAVASCRIPT', 'PYTHON', 'RUBY'];

interface WorkflowEditorLayoutProps {
    customCanvasWidth?: number;
    enableUndoRedo?: boolean;
    fitViewOnWorkflowChange?: boolean;
    includeComponents?: string[];
    internalOnlyVisible?: boolean;
    leftSidebarOpen?: boolean;
    onEditSubflowClick?: (workflowUuid: string) => void;
    runDisabled: boolean;
    showCopilot?: boolean;
    showWorkflowInputs: boolean;
    workflowReferenceId?: number | string;
}

const WorkflowEditorLayout = ({
    customCanvasWidth,
    enableUndoRedo,
    fitViewOnWorkflowChange,
    includeComponents,
    internalOnlyVisible = false,
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
    const currentNode = useWorkflowNodeDetailsPanelStore((state) => state.currentNode);
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
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);

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

    const {codeWorkflow, codeWorkflowLanguage, invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();
    const {handleClusterElementsCanvasOpenChange, isMainRootClusterElement} = useWorkflowEditorLayout();

    const queryClient = useQueryClient();
    const {integrationId, projectId, projectWorkflowId} = useParams();

    const isCodeWorkflow = useMemo(
        () =>
            codeWorkflow === true &&
            !!codeWorkflowLanguage &&
            POLYGLOT_CODE_WORKFLOW_LANGUAGES.includes(codeWorkflowLanguage),
        [codeWorkflow, codeWorkflowLanguage]
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
        // The workflow editor is shared: when opened for an integration (embedded) the Copilot uses the
        // WORKFLOW_EDITOR_EMBEDDED source, so the post-turn refresh must register for that source and invalidate the
        // integration's workflow-by-id (there is no project/projectWorkflow to invalidate).
        if (integrationId) {
            return useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR_EMBEDDED, () => {
                const workflowId = useWorkflowDataStore.getState().workflow?.id;

                if (workflowId) {
                    queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflowId)});
                }
            });
        }

        return useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_EDITOR, () => {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
        });
    }, [integrationId, projectId, projectWorkflowId, queryClient]);

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
                <div className="absolute top-2 left-2 z-10 flex flex-col gap-2">
                    <SubflowBanner />

                    <ErrorsBanner />
                </div>

                {/*
                    The right sidebar (components/flow-control drag panel), node details panel, cluster
                    dialog, and data-pill/output/input sheets below stay mounted as-is for code workflows:
                    they only render in response to ReactFlow node/cluster selection state, which never
                    populates without a canvas, so they degrade to inert rather than throwing. Trimming
                    that visual-only chrome for code workflows is left for a follow-up pass.
                */}

                {isCodeWorkflow && codeWorkflowLanguage && integrationId ? (
                    <Suspense>
                        <IntegrationCodeWorkflowDetail integrationId={integrationId} language={codeWorkflowLanguage} />
                    </Suspense>
                ) : isCodeWorkflow && codeWorkflowLanguage && projectId ? (
                    <Suspense>
                        <ProjectCodeWorkflowDetail language={codeWorkflowLanguage} projectId={projectId} />
                    </Suspense>
                ) : (
                    componentDefinitions &&
                    taskDispatcherDefinitions && (
                        <Suspense>
                            <WorkflowEditor
                                componentDefinitions={componentDefinitions}
                                customCanvasWidth={customCanvasWidth}
                                enableUndoRedo={enableUndoRedo}
                                fitViewOnWorkflowChange={fitViewOnWorkflowChange}
                                leftSidebarOpen={leftSidebarOpen}
                                taskDispatcherDefinitions={taskDispatcherDefinitions}
                            />
                        </Suspense>
                    )
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

            {currentNode?.type && !isMainRootClusterElement && !clusterElementsCanvasOpen && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation!}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            {clusterDialogMounted && (
                <Suspense fallback={null}>
                    <ClusterElementsCanvasDialog
                        onOpenChange={handleClusterElementsCanvasOpenChange}
                        open={clusterElementsCanvasOpen}
                        previousComponentDefinitions={previousComponentDefinitions}
                        updateWorkflowMutation={updateWorkflowMutation!}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                        workflowReferenceId={workflowReferenceId}
                    />
                </Suspense>
            )}

            {workflow.id && workflowTestChatPanelOpen && (
                <Suspense fallback={null}>
                    <WorkflowTestChatPanel />
                </Suspense>
            )}

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
                    internalOnlyVisible={internalOnlyVisible}
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
