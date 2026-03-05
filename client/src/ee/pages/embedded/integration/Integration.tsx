import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import IntegrationHeader from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeader';
import IntegrationsLeftSidebar from '@/ee/pages/embedded/integration/components/integrations-sidebar/IntegrationsLeftSidebar';
import {useIntegration} from '@/ee/pages/embedded/integration/hooks/useIntegration';
import {useCreateConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const Integration = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const {
        bottomResizablePanelRef,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleIntegrationClick,
        handleWorkflowExecutionsTestOutputCloseClick,
        integrationId,
        integrationWorkflowId,
        invalidateWorkflowQueries,
        leftSidebarOpen,
        sidebarLoaded,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    } = useIntegration();
    const {runDisabled} = useRun();

    return (
        <div className="flex w-full">
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            <div className="shrink-0 overflow-hidden">
                <div
                    className={twMerge(
                        'w-[355px] transition-[margin-left,opacity] duration-300 ease-out',
                        leftSidebarOpen ? 'ml-0 opacity-100' : '-ml-[355px] opacity-0'
                    )}
                >
                    {sidebarLoaded && (
                        <IntegrationsLeftSidebar
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            currentWorkflowId={workflow.id!}
                            integrationId={integrationId}
                            onIntegrationClick={handleIntegrationClick}
                        />
                    )}
                </div>
            </div>

            <div className="flex w-full flex-col">
                {integrationId && (
                    <IntegrationHeader
                        bottomResizablePanelRef={bottomResizablePanelRef}
                        chatTrigger={
                            workflow.triggers &&
                            workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                        }
                        integrationId={integrationId}
                        integrationWorkflowId={integrationWorkflowId}
                        runDisabled={runDisabled}
                        updateWorkflowMutation={updateWorkflowMutation}
                    />
                )}

                <div className="flex flex-1">
                    <ResizablePanelGroup className="flex-1 bg-surface-main" orientation="vertical">
                        <ResizablePanel className="relative flex" defaultSize={650}>
                            <WorkflowEditorProvider
                                value={{
                                    ConnectionKeys: ConnectionKeys,
                                    deleteClusterElementParameterMutation,
                                    deleteWorkflowNodeParameterMutation,
                                    invalidateWorkflowQueries,
                                    updateClusterElementParameterMutation,
                                    updateWorkflowMutation: updateWorkflowEditorMutation,
                                    updateWorkflowNodeParameterMutation,
                                    useCreateConnectionMutation: useCreateConnectionMutation,
                                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                    useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                    useGetConnectionsQuery,
                                    webhookTriggerTestApi: new WebhookTriggerTestApi(),
                                }}
                            >
                                {integrationId && (
                                    <WorkflowEditorLayout
                                        leftSidebarOpen={leftSidebarOpen}
                                        runDisabled={runDisabled}
                                        showWorkflowInputs={true}
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
        </div>
    );
};

export default Integration;
