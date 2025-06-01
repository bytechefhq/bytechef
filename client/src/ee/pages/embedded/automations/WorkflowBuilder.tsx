import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import WorkflowBuilderHeader from '@/ee/pages/embedded/automations/components/workflow-builder-header/WorkflowBuilderHeader';
import {useWorkflowBuilder} from '@/ee/pages/embedded/automations/hooks/useWorkflowBuilder';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowNodeParameterMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ConnectionReactQueryProvider} from '@/shared/components/connection/providers/connectionReactQueryProvider';
import {useCreateConnectionMutation} from '@/shared/mutations/embedded/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/shared/queries/embedded/connections.queries';

const WorkflowBuilder = () => {
    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();
    const {workflow} = useWorkflowDataStore();

    const {
        bottomResizablePanelRef,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        include,
        projectId,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
    } = useWorkflowBuilder();
    const {runDisabled} = useRun();

    return (
        <div className="flex size-full flex-col">
            {projectId && (
                <WorkflowBuilderHeader
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    chatTrigger={
                        workflow.triggers &&
                        workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                    }
                    projectId={projectId}
                    runDisabled={runDisabled}
                    updateWorkflowMutation={updateWorkflowMutation}
                />
            )}

            <div className="flex flex-1">
                <ResizablePanelGroup className="flex-1 bg-surface-main" direction="vertical">
                    <ResizablePanel className="relative flex" defaultSize={65}>
                        <ConnectionReactQueryProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                useCreateConnectionMutation: useCreateConnectionMutation,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery,
                            }}
                        >
                            <WorkflowMutationProvider
                                value={{
                                    updateWorkflowMutation: updateWorkflowEditorMutation,
                                }}
                            >
                                <WorkflowNodeParameterMutationProvider
                                    value={{
                                        deleteWorkflowNodeParameterMutation,
                                        updateWorkflowNodeParameterMutation,
                                    }}
                                >
                                    {projectId && (
                                        <WorkflowEditorLayout
                                            inputs={include}
                                            parentId={projectId}
                                            parentType="PROJECT"
                                            runDisabled={runDisabled}
                                            showWorkflowInputs={false}
                                        />
                                    )}
                                </WorkflowNodeParameterMutationProvider>
                            </WorkflowMutationProvider>
                        </ConnectionReactQueryProvider>
                    </ResizablePanel>

                    <ResizableHandle className="bg-muted" />

                    <ResizablePanel className="bg-background" defaultSize={0} ref={bottomResizablePanelRef}>
                        <WorkflowExecutionsTestOutput
                            onCloseClick={handleWorkflowExecutionsTestOutputCloseClick}
                            workflowIsRunning={workflowIsRunning}
                            workflowTestExecution={workflowTestExecution}
                        />
                    </ResizablePanel>
                </ResizablePanelGroup>
            </div>
        </div>
    );
};

export default WorkflowBuilder;
