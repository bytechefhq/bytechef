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
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useEffect} from 'react';
import {twMerge} from 'tailwind-merge';

import AiAgentWorkflowEditor from '../ai-agent-editor/components/AiAgentWorkflowEditor';
import DataPillPanel from './components/DataPillPanel';
import WorkflowEditor from './components/WorkflowEditor';
import useWorkflowDataStore from './stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from './stores/useWorkflowNodeDetailsPanelStore';

const WorkflowEditorLayout = () => {
    const {copilotPanelOpen} = useCopilotStore();
    const {projectLeftSidebarOpen} = useProjectsLeftSidebarStore();
    const {rightSidebarOpen} = useRightSidebarStore();
    const {workflow} = useWorkflowDataStore();
    const {
        aiAgentOpen,
        setAiAgentNodeData,
        setAiAgentOpen,
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
        runDisabled,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout();

    const {updateWorkflowMutation} = useWorkflowMutation();

    useEffect(() => {
        if (currentNode?.componentName === 'aiAgent') {
            setAiAgentNodeData(currentNode);
        }
    }, [currentNode, setAiAgentNodeData]);

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
                    />
                </div>
            </PageLoader>

            {currentComponent && currentComponent.componentName !== 'aiAgent' && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            <Dialog
                onOpenChange={(open) => {
                    setAiAgentOpen(open);

                    if (!open) {
                        setAiAgentNodeData(undefined);
                        useWorkflowNodeDetailsPanelStore.getState().reset();
                    }
                }}
                open={aiAgentOpen}
            >
                <DialogHeader>
                    <DialogTitle className="sr-only"></DialogTitle>

                    <DialogDescription />
                </DialogHeader>

                <DialogContent className="absolute bottom-4 left-16 top-12 h-[calc(100vh-64px)] w-[calc(100vw-80px)] max-w-none translate-x-0 translate-y-0 gap-2 bg-surface-main p-0">
                    <AiAgentWorkflowEditor />

                    <WorkflowNodeDetailsPanel
                        className="fixed inset-y-0 right-0 rounded-l-none border-none"
                        previousComponentDefinitions={previousComponentDefinitions}
                        updateWorkflowMutation={updateWorkflowMutation}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    />
                </DialogContent>
            </Dialog>

            {workflow.id && <WorkflowTestChatPanel />}

            <DataPillPanel
                isLoading={isWorkflowNodeOutputsPending}
                previousComponentDefinitions={previousComponentDefinitions}
                workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
            />

            <WorkflowInputsSheet
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
