import {ReactFlowProvider} from '@xyflow/react';

import '@xyflow/react/dist/base.css';

import './WorkflowEditorLayout.css';

import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowNodeDetailsPanel from '@/pages/platform/workflow-editor/components/WorkflowNodeDetailsPanel';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {XIcon} from 'lucide-react';
import {Suspense, lazy, useEffect, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import ClusterElementsWorkflowEditor from '../cluster-element-editor/components/ClusterElementsWorkflowEditor';
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

const DataPillPanel = lazy(() => import('./components/datapills/DataPillPanel'));
const WorkflowEditor = lazy(() => import('./components/WorkflowEditor'));
const WorkflowRightSidebar = lazy(() => import('./components/WorkflowRightSidebar'));
const WorkflowNodesSidebar = lazy(() => import('./components/WorkflowNodesSidebar'));

interface WorkflowEditorLayoutProps {
    includeComponents?: string[];
    runDisabled: boolean;
    showWorkflowInputs: boolean;
}

const WorkflowEditorLayout = ({includeComponents, runDisabled, showWorkflowInputs}: WorkflowEditorLayoutProps) => {
    const copilotPanelOpen = useCopilotStore((state) => state.copilotPanelOpen);
    const projectLeftSidebarOpen = useProjectsLeftSidebarStore((state) => state.projectLeftSidebarOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore(
        useShallow((state) => ({
            currentComponent: state.currentComponent,
            currentNode: state.currentNode,
        }))
    );
    const {
        clusterElementsCanvasOpen,
        setClusterElementsCanvasOpen,
        setRootClusterElementNodeData,
        setShowWorkflowCodeEditorSheet,
        setShowWorkflowInputsSheet,
        setShowWorkflowOutputsSheet,
        showWorkflowCodeEditorSheet,
        showWorkflowInputsSheet,
        showWorkflowOutputsSheet,
    } = useWorkflowEditorStore(
        useShallow((state) => ({
            clusterElementsCanvasOpen: state.clusterElementsCanvasOpen,
            setClusterElementsCanvasOpen: state.setClusterElementsCanvasOpen,
            setRootClusterElementNodeData: state.setRootClusterElementNodeData,
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
        previousComponentDefinitions,
        taskDispatcherDefinitions,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout(includeComponents);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const isMainRootClusterElement = useMemo(
        () => currentNode?.clusterRoot && !currentNode?.isNestedClusterRoot,
        [currentNode?.clusterRoot, currentNode?.isNestedClusterRoot]
    );

    useEffect(() => {
        if (isMainRootClusterElement) {
            setRootClusterElementNodeData(currentNode);
        }
    }, [isMainRootClusterElement, setRootClusterElementNodeData, currentNode]);

    return (
        <ReactFlowProvider>
            <div className={twMerge('relative mx-3 mb-3 flex w-full', projectLeftSidebarOpen && 'ml-0')}>
                {componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense>
                        <WorkflowEditor
                            componentDefinitions={componentDefinitions}
                            invalidateWorkflowQueries={invalidateWorkflowQueries!}
                            projectLeftSidebarOpen={projectLeftSidebarOpen}
                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                        />
                    </Suspense>
                )}

                {rightSidebarOpen && componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense fallback={<WorkflowNodesSidebarSkeleton />}>
                        <WorkflowNodesSidebar
                            data={{
                                componentDefinitions,
                                taskDispatcherDefinitions,
                            }}
                        />
                    </Suspense>
                )}

                {componentDefinitions && taskDispatcherDefinitions && (
                    <Suspense fallback={<WorkflowRightSidebarSkeleton />}>
                        <WorkflowRightSidebar
                            copilotPanelOpen={copilotPanelOpen}
                            onComponentsAndFlowControlsClick={handleComponentsAndFlowControlsClick}
                            onCopilotClick={handleCopilotClick}
                            onWorkflowCodeEditorClick={handleWorkflowCodeEditorClick}
                            onWorkflowInputsClick={handleWorkflowInputsClick}
                            onWorkflowOutputsClick={handleWorkflowOutputsClick}
                            rightSidebarOpen={rightSidebarOpen}
                            showWorkflowInputs={showWorkflowInputs}
                        />
                    </Suspense>
                )}
            </div>

            {currentComponent && !isMainRootClusterElement && (
                <WorkflowNodeDetailsPanel
                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation!}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            {clusterElementsCanvasOpen && (
                <Dialog
                    onOpenChange={(open) => {
                        setClusterElementsCanvasOpen(open);

                        if (!open) {
                            setRootClusterElementNodeData(undefined);
                            useWorkflowNodeDetailsPanelStore.getState().reset();
                        }
                    }}
                    open={clusterElementsCanvasOpen}
                >
                    <DialogHeader>
                        <DialogTitle className="sr-only"></DialogTitle>

                        <DialogDescription />
                    </DialogHeader>

                    <DialogContent className="absolute bottom-4 left-16 top-12 h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 gap-2 bg-surface-main p-0">
                        <ClusterElementsWorkflowEditor />

                        <WorkflowNodeDetailsPanel
                            className="fixed inset-y-0 right-0 rounded-l-none"
                            closeButton={
                                <DialogClose asChild>
                                    <Button
                                        className="absolute right-2 top-2"
                                        size="icon"
                                        title="Close the canvas"
                                        variant="ghost"
                                    >
                                        <XIcon />
                                    </Button>
                                </DialogClose>
                            }
                            invalidateWorkflowQueries={invalidateWorkflowQueries!}
                            previousComponentDefinitions={previousComponentDefinitions}
                            updateWorkflowMutation={updateWorkflowMutation!}
                            workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                        />

                        {dataPillPanelOpen && (
                            <Suspense fallback={<DataPillPanelSkeleton />}>
                                <DataPillPanel
                                    className="fixed inset-y-0 right-[465px] rounded-none"
                                    previousComponentDefinitions={previousComponentDefinitions}
                                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                                />
                            </Suspense>
                        )}
                    </DialogContent>
                </Dialog>
            )}

            {workflow.id && <WorkflowTestChatPanel />}

            {currentComponent && !isMainRootClusterElement && dataPillPanelOpen && (
                <Suspense fallback={<DataPillPanelSkeleton />}>
                    <DataPillPanel
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
