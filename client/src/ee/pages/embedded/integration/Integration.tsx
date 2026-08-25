import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import IntegrationHeader from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeader';
import IntegrationsLeftSidebar from '@/ee/pages/embedded/integration/components/integrations-sidebar/IntegrationsLeftSidebar';
import {useIntegration} from '@/ee/pages/embedded/integration/hooks/useIntegration';
import {Integration as IntegrationType, WebhookTriggerTestApi} from '@/ee/shared/middleware/embedded/configuration';
import {useCreateConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import {useGetIntegrationQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useLoaderData} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const Integration = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );
    const showBottomPanel = useWorkflowEditorStore((state) => state.showBottomPanel);

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const {
        bottomResizablePanelRef,
        cancelWorkflowQueries,
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

    const {data: integration} = useGetIntegrationQuery(integrationId, useLoaderData() as IntegrationType);

    const {runDisabled} = useRun();

    const copilotLayoutShifted = useCopilotLayoutShifted();

    return (
        <div className="flex w-full">
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            {/* A code workflow has no editable workflow tree, but the sidebar still lists its generated workflows
             * and its sibling integrations, so it stays mounted — only workflow creation is hidden inside it. */}

            <div className="h-full shrink-0 overflow-hidden">
                <div
                    className={twMerge(
                        'h-full w-[355px] transition-[margin-left,opacity] duration-300 ease-out',
                        leftSidebarOpen ? 'ml-0 opacity-100' : 'ml-[-355px] opacity-0'
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
                                    cancelWorkflowQueries,
                                    codeWorkflow: integration?.codeWorkflow,
                                    codeWorkflowLanguage: integration?.codeWorkflowLanguage,
                                    connectionTagsQueryKey: ConnectionKeys.connectionTags,
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
                                        internalOnlyVisible={true}
                                        leftSidebarOpen={leftSidebarOpen}
                                        runDisabled={runDisabled}
                                        showWorkflowInputs={true}
                                        workflowReferenceId={integrationWorkflowId}
                                    />
                                )}
                            </WorkflowEditorProvider>
                        </ResizablePanel>

                        <ResizableHandle className="bg-muted" />

                        <ResizablePanel className="flex" defaultSize={0} panelRef={bottomResizablePanelRef}>
                            {(showBottomPanel || workflowIsRunning || workflowTestExecution) && (
                                <div
                                    className={twMerge(
                                        'm-3 flex flex-1 overflow-hidden rounded-lg bg-background',
                                        leftSidebarOpen && 'ml-0',
                                        copilotLayoutShifted && 'mr-0'
                                    )}
                                >
                                    <WorkflowExecutionsTestOutput
                                        onCloseClick={handleWorkflowExecutionsTestOutputCloseClick}
                                        workflowIsRunning={workflowIsRunning}
                                        workflowTestExecution={workflowTestExecution}
                                    />
                                </div>
                            )}
                        </ResizablePanel>
                    </ResizablePanelGroup>
                </div>
            </div>
        </div>
    );
};

export default Integration;
