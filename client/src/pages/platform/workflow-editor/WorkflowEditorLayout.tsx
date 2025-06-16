import {ReactFlowProvider} from '@xyflow/react';

import WorkflowNodeDetailsPanel from './components/WorkflowNodeDetailsPanel';

import '@xyflow/react/dist/base.css';

import './WorkflowEditorLayout.css';

import PageLoader from '@/components/PageLoader';
import {Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet';
import WorkflowNodesSidebar from '@/pages/platform/workflow-editor/components/WorkflowNodesSidebar';
import WorkflowOutputsSheet from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheet';
import WorkflowRightSidebar from '@/pages/platform/workflow-editor/components/WorkflowRightSidebar';
import WorkflowInputsSheet from '@/pages/platform/workflow-editor/components/workflow-inputs/WorkflowInputsSheet';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ROOT_CLUSTER_ELEMENT_NAMES} from '@/shared/constants';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {twMerge} from 'tailwind-merge';

import ClusterElementsWorkflowEditor from '../cluster-element-editor/components/ClusterElementsWorkflowEditor';
import WorkflowEditor from './components/WorkflowEditor';
import DataPillPanel from './components/datapills/DataPillPanel';
import useWorkflowDataStore from './stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from './stores/useWorkflowNodeDetailsPanelStore';
import saveClusterElementNodesPosition from './utils/saveClusterElementNodesPosition';

const WorkflowEditorLayout = ({
    includeComponents,
    runDisabled,
    showWorkflowInputs,
}: {
    includeComponents?: string[];
    runDisabled: boolean;
    showWorkflowInputs: boolean;
}) => {
    const {copilotPanelOpen} = useCopilotStore();
    const {projectLeftSidebarOpen} = useProjectsLeftSidebarStore();
    const {rightSidebarOpen} = useRightSidebarStore();
    const {workflow} = useWorkflowDataStore();
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
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore();

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
        isWorkflowNodeOutputsPending,
        previousComponentDefinitions,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout(includeComponents);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const isRootClusterElement = ROOT_CLUSTER_ELEMENT_NAMES.includes(currentComponent?.componentName as string);

    const queryClient = useQueryClient();

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
                        <WorkflowEditor
                            componentDefinitions={componentDefinitions}
                            invalidateWorkflowQueries={invalidateWorkflowQueries!}
                            projectLeftSidebarOpen={projectLeftSidebarOpen}
                            taskDispatcherDefinitions={taskDispatcherDefinitions}
                        />
                    )}

                    {rightSidebarOpen && componentDefinitions && taskDispatcherDefinitions && (
                        <WorkflowNodesSidebar
                            data={{
                                componentDefinitions,
                                taskDispatcherDefinitions,
                            }}
                        />
                    )}

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
                </div>
            </PageLoader>

            {currentComponent && !isRootClusterElement && (
                <WorkflowNodeDetailsPanel
                    invalidateWorkflowQueries={invalidateWorkflowQueries!}
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation!}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            <Dialog
                onOpenChange={(open) => {
                    setClusterElementsCanvasOpen(open);

                    if (!open) {
                        saveClusterElementNodesPosition({
                            invalidateWorkflowQueries: invalidateWorkflowQueries!,
                            queryClient,
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
                            <WorkflowNodeDetailsPanel
                                className="fixed inset-y-0 right-0 rounded-l-none border-none"
                                invalidateWorkflowQueries={invalidateWorkflowQueries!}
                                previousComponentDefinitions={previousComponentDefinitions}
                                updateWorkflowMutation={updateWorkflowMutation!}
                                workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                            />

                            <DataPillPanel
                                className="fixed inset-y-0 right-[465px] rounded-none"
                                isLoading={isWorkflowNodeOutputsPending}
                                previousComponentDefinitions={previousComponentDefinitions}
                                workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                            />
                        </>
                    )}
                </DialogContent>
            </Dialog>

            {workflow.id && <WorkflowTestChatPanel />}

            {currentComponent && !isRootClusterElement && (
                <DataPillPanel
                    isLoading={isWorkflowNodeOutputsPending}
                    previousComponentDefinitions={previousComponentDefinitions}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            <WorkflowInputsSheet
                invalidateWorkflowQueries={invalidateWorkflowQueries!}
                onSheetOpenChange={setShowWorkflowInputsSheet}
                sheetOpen={showWorkflowInputsSheet}
                workflowTestConfiguration={workflowTestConfiguration}
            />

            <WorkflowOutputsSheet
                onSheetOpenChange={setShowWorkflowOutputsSheet}
                sheetOpen={showWorkflowOutputsSheet}
                workflow={workflow}
            />

            <WorkflowCodeEditorSheet
                invalidateWorkflowQueries={invalidateWorkflowQueries!}
                onSheetOpenClose={setShowWorkflowCodeEditorSheet}
                runDisabled={runDisabled}
                sheetOpen={showWorkflowCodeEditorSheet}
                testConfigurationDisabled={testConfigurationDisabled}
                workflow={workflow}
                workflowTestConfiguration={workflowTestConfiguration}
            />
        </ReactFlowProvider>
    );
};

export default WorkflowEditorLayout;
