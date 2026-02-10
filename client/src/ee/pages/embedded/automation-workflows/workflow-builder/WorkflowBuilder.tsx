import LoadingDots from '@/components/LoadingDots';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import WorkflowBuilderHeader from '@/ee/pages/embedded/automation-workflows/workflow-builder/components/workflow-builder-header/WorkflowBuilderHeader';
import {useWorkflowBuilder} from '@/ee/pages/embedded/automation-workflows/workflow-builder/hooks/useWorkflowBuilder';
import {getCreateConnectedUserProjectWorkflowConnection} from '@/ee/shared/mutations/embedded/connections.mutations';
import {
    ConnectionKeys,
    getConnectedUserConnectionsQuery,
    useGetConnectionTagsQuery,
} from '@/ee/shared/queries/embedded/connections.queries';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useShallow} from 'zustand/react/shallow';

const WorkflowBuilder = () => {
    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore(
        useShallow((state) => ({
            workflowIsRunning: state.workflowIsRunning,
            workflowTestExecution: state.workflowTestExecution,
        }))
    );
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const {
        bottomResizablePanelRef,
        connectedUserProjectWorkflow,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        includeComponents,
        initialized,
        projectId,
        sharedConnectionIds,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        workflowUuid,
    } = useWorkflowBuilder();

    const {runDisabled} = useRun();

    if (!initialized) {
        return (
            <div className="flex size-full items-center justify-center">
                <LoadingDots />
            </div>
        );
    }

    if (!connectedUserProjectWorkflow || !projectId) {
        return <></>;
    }

    return (
        <div className="flex size-full flex-col">
            <WorkflowBuilderHeader
                bottomResizablePanelRef={bottomResizablePanelRef}
                chatTrigger={
                    workflow.triggers && workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                }
                projectId={projectId}
                runDisabled={runDisabled}
                updateWorkflowMutation={updateWorkflowMutation}
                workflowVersion={connectedUserProjectWorkflow?.workflowVersion}
            />

            <div className="flex flex-1">
                <ResizablePanelGroup className="flex-1 bg-transparent" orientation="vertical">
                    <ResizablePanel className="relative flex" defaultSize={650}>
                        <WorkflowEditorProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                deleteClusterElementParameterMutation,
                                deleteWorkflowNodeParameterMutation,
                                invalidateWorkflowQueries: () => {},
                                updateClusterElementParameterMutation,
                                updateWorkflowMutation: updateWorkflowEditorMutation,
                                updateWorkflowNodeParameterMutation,
                                useCreateConnectionMutation: getCreateConnectedUserProjectWorkflowConnection(
                                    connectedUserProjectWorkflow.connectedUserId!,
                                    workflowUuid!
                                ),
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery: getConnectedUserConnectionsQuery(
                                    connectedUserProjectWorkflow.connectedUserId!,
                                    sharedConnectionIds ? sharedConnectionIds : []
                                ),
                                webhookTriggerTestApi: new WebhookTriggerTestApi(),
                            }}
                        >
                            {projectId && (
                                <WorkflowEditorLayout
                                    includeComponents={includeComponents}
                                    runDisabled={runDisabled}
                                    showWorkflowInputs={false}
                                />
                            )}
                        </WorkflowEditorProvider>
                    </ResizablePanel>

                    <ResizableHandle className="bg-muted" />

                    <ResizablePanel className="bg-background" defaultSize={0} panelRef={bottomResizablePanelRef}>
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
