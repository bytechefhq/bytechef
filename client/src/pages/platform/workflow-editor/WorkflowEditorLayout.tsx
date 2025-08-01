import {ReactFlowProvider} from '@xyflow/react';

import '@xyflow/react/dist/base.css';

import './WorkflowEditorLayout.css';

import PageLoader from '@/components/PageLoader';
import {Button} from '@/components/ui/button';
import {Dialog, DialogClose, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ROOT_CLUSTER_ELEMENT_NAMES} from '@/shared/constants';
import {XIcon} from 'lucide-react';
import {Suspense, lazy, useEffect} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/shallow';

import ClusterElementsWorkflowEditor from '../cluster-element-editor/components/ClusterElementsWorkflowEditor';
import {
    DataPillPanelSkeleton,
    RightSidebarSkeleton,
    SheetSkeleton,
    WorkflowNodeDetailsPanelSkeleton,
} from './components/WorkflowEditorSkeletons';
import useDataPillPanelStore from './stores/useDataPillPanelStore';
import useWorkflowDataStore from './stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from './stores/useWorkflowNodeDetailsPanelStore';
import saveClusterElementNodesPosition from './utils/saveClusterElementNodesPosition';

const WorkflowNodeDetailsPanel = lazy(() => import('./components/WorkflowNodeDetailsPanel'));
const DataPillPanel = lazy(() => import('./components/datapills/DataPillPanel'));
const WorkflowOutputsSheet = lazy(() => import('./components/WorkflowOutputsSheet'));
const WorkflowInputsSheet = lazy(() => import('./components/workflow-inputs/WorkflowInputsSheet'));
const WorkflowCodeEditorSheet = lazy(() => import('./components/WorkflowCodeEditorSheet'));
const WorkflowEditor = lazy(() => import('./components/WorkflowEditor'));
const WorkflowRightSidebar = lazy(() => import('./components/WorkflowRightSidebar'));
const WorkflowNodesSidebar = lazy(() => import('./components/WorkflowNodesSidebar'));

interface WorkflowEditorLayoutProps {
    includeComponents?: string[];
    runDisabled: boolean;
    showWorkflowInputs: boolean;
}

const WorkflowEditorLayout = ({includeComponents, runDisabled, showWorkflowInputs}: WorkflowEditorLayoutProps) => {
    const {copilotPanelOpen} = useCopilotStore();
    const {projectLeftSidebarOpen} = useProjectsLeftSidebarStore();
    const {rightSidebarOpen} = useRightSidebarStore();
    const {workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore();

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
    } = useWorkflowEditorStore();

    const {dataPillPanelOpen} = useDataPillPanelStore(
        useShallow((state) => ({
            dataPillPanelOpen: state.dataPillPanelOpen,
        }))
    );

    const {
        componentDefinitions,
        componentsError,
        componentsIsLoading,
        filteredWorkflowNodeOutputs,
        handleComponentsAndFlowControlsClick,
        handleCopilotClick,
        handleWorkflowCodeEditorClick,
        handleWorkflowInputsClick,
        handleWorkflowOutputsClick,
        previousComponentDefinitions,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout(includeComponents);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const isRootClusterElement = ROOT_CLUSTER_ELEMENT_NAMES.includes(currentComponent?.componentName as string);

    useEffect(() => {
        if (currentNode?.rootClusterElement) {
            setRootClusterElementNodeData(currentNode);
        }
    }, [currentNode, setRootClusterElementNodeData]);

    return (
        <ReactFlowProvider>
            <PageLoader
                errors={[componentsError, taskDispatcherDefinitionsError]}
                loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
            >
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
                        <Suspense fallback={<RightSidebarSkeleton />}>
                            <WorkflowNodesSidebar
                                data={{
                                    componentDefinitions,
                                    taskDispatcherDefinitions,
                                }}
                            />
                        </Suspense>
                    )}

                    <Suspense fallback={<RightSidebarSkeleton />}>
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
                </div>
            </PageLoader>

            {currentComponent && !isRootClusterElement && (
                <Suspense fallback={<WorkflowNodeDetailsPanelSkeleton />}>
                    <WorkflowNodeDetailsPanel
                        invalidateWorkflowQueries={invalidateWorkflowQueries!}
                        previousComponentDefinitions={previousComponentDefinitions}
                        updateWorkflowMutation={updateWorkflowMutation!}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    />
                </Suspense>
            )}

            <Dialog
                onOpenChange={(open) => {
                    setClusterElementsCanvasOpen(open);

                    if (!open) {
                        saveClusterElementNodesPosition({
                            invalidateWorkflowQueries: invalidateWorkflowQueries!,
                            updateWorkflowMutation: updateWorkflowMutation!,
                            workflow,
                        });

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

                    {currentComponent && (
                        <>
                            <Suspense fallback={<WorkflowNodeDetailsPanelSkeleton />}>
                                <WorkflowNodeDetailsPanel
                                    className="fixed inset-y-0 right-0 rounded-l-none border-none"
                                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                                    previousComponentDefinitions={previousComponentDefinitions}
                                    updateWorkflowMutation={updateWorkflowMutation!}
                                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                                />
                            </Suspense>

                            {dataPillPanelOpen && (
                                <Suspense fallback={<DataPillPanelSkeleton />}>
                                    <DataPillPanel
                                        className="fixed inset-y-0 right-[465px] rounded-none"
                                        previousComponentDefinitions={previousComponentDefinitions}
                                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                                    />
                                </Suspense>
                            )}
                        </>
                    )}

                    <DialogClose asChild>
                        <Button
                            className="absolute right-2 top-2 size-10 border bg-white p-2 shadow-none [&_svg]:size-5"
                            title="Close the canvas"
                            variant="ghost"
                        >
                            <XIcon />
                        </Button>
                    </DialogClose>
                </DialogContent>
            </Dialog>

            {workflow.id && <WorkflowTestChatPanel />}

            {currentComponent && !isRootClusterElement && dataPillPanelOpen && (
                <Suspense fallback={<DataPillPanelSkeleton />}>
                    <DataPillPanel
                        previousComponentDefinitions={previousComponentDefinitions}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    />
                </Suspense>
            )}

            <Suspense fallback={<SheetSkeleton />}>
                <WorkflowInputsSheet
                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                    onSheetOpenChange={setShowWorkflowInputsSheet}
                    sheetOpen={showWorkflowInputsSheet}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            </Suspense>

            <Suspense fallback={<SheetSkeleton />}>
                <WorkflowOutputsSheet
                    onSheetOpenChange={setShowWorkflowOutputsSheet}
                    sheetOpen={showWorkflowOutputsSheet}
                    workflow={workflow}
                />
            </Suspense>

            <Suspense fallback={<SheetSkeleton />}>
                <WorkflowCodeEditorSheet
                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                    onSheetOpenClose={setShowWorkflowCodeEditorSheet}
                    runDisabled={runDisabled}
                    sheetOpen={showWorkflowCodeEditorSheet}
                    testConfigurationDisabled={testConfigurationDisabled}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            </Suspense>
        </ReactFlowProvider>
    );
};

export default WorkflowEditorLayout;
