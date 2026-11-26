import LoadingDots from '@/components/LoadingDots';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import WorkflowBuilderHeader from '@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/WorkflowBuilderHeader';
import {useWorkflowBuilder} from '@/ee/pages/embedded/workflow-builder/hooks/useWorkflowBuilder';
import {getCreateConnectedUserConnection} from '@/ee/shared/mutations/embedded/connections.mutations';
import {ConnectedUserProjectWorkflowKeys} from '@/ee/shared/queries/embedded/connectedUserProjectWorkflows.queries';
import {
    ConnectionKeys,
    getConnectedUserConnectionsQuery,
    useGetConnectionTagsQuery,
} from '@/ee/shared/queries/embedded/connections.queries';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import useTestRunChatTrigger from '@/pages/platform/workflow-editor/hooks/useTestRunChatTrigger';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {
    Source,
    WorkflowBuilderCopilotParametersI,
    useCopilotStore,
} from '@/shared/components/copilot/stores/useCopilotStore';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {PlatformType} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';
import {useParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

const WorkflowBuilder = () => {
    const {showBottomPanel, workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore(
        useShallow((state) => ({
            showBottomPanel: state.showBottomPanel,
            workflowIsRunning: state.workflowIsRunning,
            workflowTestExecution: state.workflowTestExecution,
        }))
    );
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const {
        bottomResizablePanelRef,
        cancelWorkflowQueries,
        connectedUserProjectWorkflow,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        includeComponents,
        initialized,
        invalidateWorkflowQueries,
        projectId,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
    } = useWorkflowBuilder();

    const {runDisabled} = useRun();

    const chatTrigger = useTestRunChatTrigger(workflow);

    const queryClient = useQueryClient();

    const {workflowUuid} = useParams();

    useEffect(() => {
        return useCopilotPostTurnRegistry.getState().register(Source.WORKFLOW_BUILDER_EMBEDDED, () => {
            if (workflowUuid) {
                void queryClient.invalidateQueries({
                    queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow(workflowUuid),
                });
            }

            void queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});
        });
    }, [queryClient, workflowUuid]);

    useEffect(() => {
        const {context, setContext} = useCopilotStore.getState();

        if (!workflowUuid || context.source !== Source.WORKFLOW_BUILDER_EMBEDDED) {
            return;
        }

        if ((context.parameters as WorkflowBuilderCopilotParametersI).workflowUuid !== workflowUuid) {
            setContext({...context, parameters: {workflowUuid}});
        }
    }, [workflowUuid]);

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
                chatTrigger={chatTrigger}
                projectId={projectId}
                runDisabled={runDisabled}
                updateWorkflowMutation={updateWorkflowMutation}
                workflowVersion={connectedUserProjectWorkflow?.workflowVersion}
            />

            <div className="flex min-w-0 flex-1">
                <ResizablePanelGroup className="min-w-0 flex-1 bg-transparent" orientation="vertical">
                    <ResizablePanel className="relative flex" defaultSize={650}>
                        <WorkflowEditorProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                cancelWorkflowQueries,
                                connectedUserWorkflowUuid: workflowUuid,
                                deleteClusterElementParameterMutation,
                                deleteWorkflowNodeParameterMutation,
                                includeComponents,
                                invalidateWorkflowQueries,
                                platformType: PlatformType.Automation,
                                updateClusterElementParameterMutation,
                                updateWorkflowMutation: updateWorkflowEditorMutation,
                                updateWorkflowNodeParameterMutation,
                                useCreateConnectionMutation: getCreateConnectedUserConnection(
                                    connectedUserProjectWorkflow.connectedUserId!
                                ),
                                useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery: getConnectedUserConnectionsQuery(
                                    connectedUserProjectWorkflow.connectedUserId!
                                ),
                                webhookTriggerTestApi: new WebhookTriggerTestApi(),
                            }}
                        >
                            {!!projectId && (
                                <WorkflowEditorLayout
                                    includeComponents={includeComponents}
                                    runDisabled={runDisabled}
                                    showCopilot={copilotEnabled}
                                    showWorkflowInputs={false}
                                />
                            )}
                        </WorkflowEditorProvider>
                    </ResizablePanel>

                    <ResizableHandle className="bg-muted" />

                    <ResizablePanel className="flex" defaultSize={0} panelRef={bottomResizablePanelRef}>
                        {(showBottomPanel ||
                            workflowIsRunning ||
                            workflowTestExecution ||
                            workflowTestChatPanelOpen) && (
                            <div className="m-3 flex flex-1 overflow-hidden rounded-lg bg-background">
                                {(showBottomPanel || workflowIsRunning || workflowTestExecution) && (
                                    <WorkflowExecutionsTestOutput
                                        onCloseClick={handleWorkflowExecutionsTestOutputCloseClick}
                                        workflowIsRunning={workflowIsRunning}
                                        workflowTestExecution={workflowTestExecution}
                                    />
                                )}
                            </div>
                        )}
                    </ResizablePanel>
                </ResizablePanelGroup>

                {copilotEnabled && (
                    <aside className="h-full shrink-0">
                        <CopilotPanel open={copilotPanelOpen} />
                    </aside>
                )}
            </div>
        </div>
    );
};

export default WorkflowBuilder;
