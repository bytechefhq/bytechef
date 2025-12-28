import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import IntegrationHeader from '@/ee/pages/embedded/integration/components/integration-header/IntegrationHeader';
import IntegrationsSidebar from '@/ee/pages/embedded/integration/components/integrations-sidebar/IntegrationsSidebar';
import IntegrationsSidebarHeader from '@/ee/pages/embedded/integration/components/integrations-sidebar/IntegrationsSidebarHeader';
import {useIntegration} from '@/ee/pages/embedded/integration/hooks/useIntegration';
import useIntegrationsLeftSidebarStore from '@/ee/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import {useCreateConnectionMutation} from '@/ee/shared/mutations/embedded/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/ee/shared/queries/embedded/connections.queries';
import {IntegrationWorkflowKeys} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const Integration = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {leftSidebarOpen} = useIntegrationsLeftSidebarStore(
        useShallow((state) => ({
            leftSidebarOpen: state.leftSidebarOpen,
        }))
    );
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const {integrationId, integrationWorkflowId} = useParams();

    const queryClient = useQueryClient();

    const {
        bottomResizablePanelRef,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
    } = useIntegration({
        integrationId: parseInt(integrationId!),
        integrationWorkflowId: parseInt(integrationWorkflowId!),
    });

    const {runDisabled} = useRun();

    return (
        <>
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />
            <LayoutContainer
                className="bg-muted/50"
                leftSidebarBody={<IntegrationsSidebar integrationId={+integrationId!} />}
                leftSidebarClass="bg-background"
                leftSidebarHeader={<Header right={<IntegrationsSidebarHeader />} title="Integrations" />}
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="96"
                topHeader={
                    integrationId && (
                        <IntegrationHeader
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            integrationId={parseInt(integrationId)}
                            integrationWorkflowId={parseInt(integrationWorkflowId!)}
                            runDisabled={runDisabled}
                            updateWorkflowMutation={updateWorkflowMutation}
                        />
                    )
                }
            >
                <ResizablePanelGroup className="flex-1 bg-surface-main" direction="vertical">
                    <ResizablePanel className="relative flex" defaultSize={65}>
                        <WorkflowEditorProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                deleteClusterElementParameterMutation,
                                deleteWorkflowNodeParameterMutation,
                                invalidateWorkflowQueries: () => {
                                    queryClient.invalidateQueries({
                                        queryKey: IntegrationWorkflowKeys.integrationWorkflows(+integrationId!),
                                    });
                                },
                                updateClusterElementParameterMutation,
                                updateWorkflowMutation: updateWorkflowEditorMutation,
                                updateWorkflowNodeParameterMutation,
                                useCreateConnectionMutation: useCreateConnectionMutation,
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery: useGetConnectionsQuery,
                                webhookTriggerTestApi: new WebhookTriggerTestApi(),
                            }}
                        >
                            {integrationId && (
                                <WorkflowEditorLayout runDisabled={runDisabled} showWorkflowInputs={true} />
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
            </LayoutContainer>
        </>
    );
};

export default Integration;
