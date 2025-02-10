import {ReactFlowProvider} from '@xyflow/react';

import WorkflowNodeDetailsPanel from './components/WorkflowNodeDetailsPanel';

import '@xyflow/react/dist/base.css';

import './WorkflowEditorLayout.css';

import PageLoader from '@/components/PageLoader';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/components/WorkflowCodeEditorSheet';
import WorkflowInputsSheet from '@/pages/platform/workflow-editor/components/WorkflowInputsSheet';
import WorkflowNodesSidebar from '@/pages/platform/workflow-editor/components/WorkflowNodesSidebar';
import WorkflowOutputsSheet from '@/pages/platform/workflow-editor/components/WorkflowOutputsSheet';
import WorkflowRightSidebar from '@/pages/platform/workflow-editor/components/WorkflowRightSidebar';
import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';

import DataPillPanel from './components/DataPillPanel';
import WorkflowEditor from './components/WorkflowEditor';
import useWorkflowDataStore from './stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from './stores/useWorkflowNodeDetailsPanelStore';

const WorkflowEditorLayout = () => {
    const {copilotPanelOpen} = useCopilotStore();
    const {leftSidebarOpen} = useProjectsLeftSidebarStore();
    const {rightSidebarOpen} = useRightSidebarStore();
    const {workflow} = useWorkflowDataStore();
    const {
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
        previousComponentDefinitions,
        runDisabled,
        taskDispatcherDefinitions,
        taskDispatcherDefinitionsError,
        taskDispatcherDefinitionsLoading,
        testConfigurationDisabled,
        workflowTestConfiguration,
    } = useWorkflowLayout();

    const {updateWorkflowMutation} = useWorkflowMutation();

    return (
        <ReactFlowProvider>
            <PageLoader
                errors={[componentsError, taskDispatcherDefinitionsError]}
                loading={componentsIsLoading || taskDispatcherDefinitionsLoading}
            >
                <div className="flex size-full">
                    {componentDefinitions && taskDispatcherDefinitions && (
                        <WorkflowEditor
                            componentDefinitions={componentDefinitions}
                            leftSidebarOpen={leftSidebarOpen}
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

            {currentComponent && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            {workflow.id && <WorkflowTestChatPanel />}

            {(filteredWorkflowNodeOutputs || (!filteredWorkflowNodeOutputs && currentNode?.trigger)) &&
                previousComponentDefinitions && (
                    <DataPillPanel
                        previousComponentDefinitions={previousComponentDefinitions}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    />
                )}

            <WorkflowInputsSheet
                onSheetOpenChange={setShowWorkflowInputsSheet}
                sheetOpen={showWorkflowInputsSheet}
                workflow={workflow}
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
