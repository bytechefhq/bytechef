import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import IntegrationHeader from '@/pages/embedded/integration/components/integration-header/IntegrationHeader';
import IntegrationsSidebar from '@/pages/embedded/integration/components/integrations-sidebar/IntegrationsSidebar';
import IntegrationsSidebarHeader from '@/pages/embedded/integration/components/integrations-sidebar/IntegrationsSidebarHeader';
import {useIntegration} from '@/pages/embedded/integration/hooks/useIntegration';
import useIntegrationsLeftSidebarStore from '@/pages/embedded/integration/stores/useIntegrationsLeftSidebarStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowNodeParameterMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ConnectionReactQueryProvider} from '@/shared/components/connection/providers/connectionReactQueryProvider';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useCreateConnectionMutation} from '@/shared/mutations/embedded/connections.mutations';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetConnectionsQuery,
} from '@/shared/queries/embedded/connections.queries';
import {useParams} from 'react-router-dom';

const Integration = () => {
    const {leftSidebarOpen} = useIntegrationsLeftSidebarStore();
    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();

    const {integrationId, integrationWorkflowId} = useParams();

    const {
        bottomResizablePanelRef,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
    } = useIntegration({
        integrationId: parseInt(integrationId!),
        integrationWorkflowId: parseInt(integrationWorkflowId!),
    });

    const {runDisabled} = useWorkflowLayout();

    return (
        <>
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
                <ResizablePanelGroup className="flex-1" direction="vertical">
                    <ResizablePanel className="relative" defaultSize={65}>
                        <ConnectionReactQueryProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                useCreateConnectionMutation: useCreateConnectionMutation,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery: useGetConnectionsQuery,
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
                                    <WorkflowEditorLayout />
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
            </LayoutContainer>
        </>
    );
};

export default Integration;
