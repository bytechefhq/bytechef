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

const WorkflowBuilder = () => {
    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();
    const {workflow} = useWorkflowDataStore();

    const {
        bottomResizablePanelRef,
        connectedUserProjectWorkflow,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        includeComponents,
        projectId,
        sharedConnectionIds,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        workflowReferenceCode,
    } = useWorkflowBuilder();

    const {runDisabled} = useRun();

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
                <ResizablePanelGroup className="flex-1 bg-transparent" direction="vertical">
                    <ResizablePanel className="relative flex" defaultSize={65}>
                        <WorkflowEditorProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                deleteWorkflowNodeParameterMutation,
                                invalidateWorkflowQueries: () => {},
                                updateWorkflowMutation: updateWorkflowEditorMutation,
                                updateWorkflowNodeParameterMutation,
                                useCreateConnectionMutation: getCreateConnectedUserProjectWorkflowConnection(
                                    connectedUserProjectWorkflow.connectedUserId!,
                                    workflowReferenceCode!
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
