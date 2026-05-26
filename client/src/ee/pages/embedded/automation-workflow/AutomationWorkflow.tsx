import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import AutomationWorkflowEditorHeader from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/AutomationWorkflowEditorHeader';
import AutomationWorkflowEditorLeftSidebar from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/AutomationWorkflowEditorLeftSidebar';
import {useAutomationWorkflowEditorSidebarStore} from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/stores/useAutomationWorkflowEditorSidebarStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {RequestI, WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useAutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useUpdateWorkflowMutation} from '@/shared/mutations/automation/workflows.mutations';
import {
    useDeleteClusterElementParameterMutation,
    useDeleteWorkflowNodeParameterMutation,
    useUpdateClusterElementParameterMutation,
    useUpdateWorkflowNodeParameterMutation,
} from '@/shared/mutations/platform/workflowNodeParameters.mutations';
import useUpdatePlatformWorkflowMutation from '@/shared/mutations/platform/workflows.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    ConnectionKeys,
    useGetConnectionTagsQuery,
    useGetWorkspaceConnectionsQuery,
} from '@/shared/queries/automation/connections.queries';
import {WorkflowKeys, useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useRef} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';
import {useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const AutomationWorkflow = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const {setWorkflow, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            setWorkflow: state.setWorkflow,
            workflow: state.workflow,
        }))
    );

    const leftSidebarOpen = useAutomationWorkflowEditorSidebarStore((state) => state.leftSidebarOpen);

    const bottomResizablePanelRef = useRef<PanelImperativeHandle>(null);

    const {workflowId} = useParams();

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const queryClient = useQueryClient();

    const {data: currentWorkflow, isLoading: isWorkflowLoading} = useGetWorkflowQuery(workflowId!, !!workflowId);

    const {data: projectsData} = useAutomationWorkflowProjectsQuery();

    const projects = projectsData?.automationWorkflowProjects ?? [];

    const currentProject = projects.find((automationWorkflowProject) =>
        automationWorkflowProject.workflowTemplates.some(
            (projectWorkflow) => projectWorkflow.workflowUuid === workflowId
        )
    );

    const projectId = currentProject?.id ?? '';

    const {runDisabled} = useRun();

    const useGetConnectionsQuery = (request: RequestI, enabled?: boolean) =>
        useGetWorkspaceConnectionsQuery(
            {
                environmentId: currentEnvironmentId,
                id: currentWorkspaceId!,
                ...request,
            },
            enabled
        );

    const deleteWorkflowNodeParameterMutation = useDeleteWorkflowNodeParameterMutation();

    const deleteClusterElementParameterMutation = useDeleteClusterElementParameterMutation();

    const updateClusterElementParameterMutation = useUpdateClusterElementParameterMutation();

    const updateWorkflowNodeParameterMutation = useUpdateWorkflowNodeParameterMutation();

    const updateWorkflowEditorMutation = useUpdatePlatformWorkflowMutation({
        useUpdateWorkflowMutation,
        workflowId: workflow.id!,
        workflowKeys: WorkflowKeys,
    });

    const cancelWorkflowQueries = () => queryClient.cancelQueries({queryKey: WorkflowKeys.workflow(workflowId!)});

    const invalidateWorkflowQueries = () =>
        queryClient.invalidateQueries({queryKey: WorkflowKeys.workflow(workflowId!)});

    const handleWorkflowExecutionsTestOutputCloseClick = () => {
        bottomResizablePanelRef.current?.resize(0);
    };

    useEffect(() => {
        if (currentWorkflow && !isWorkflowLoading) {
            const timeoutId = setTimeout(() => {
                setWorkflow({...currentWorkflow});
            }, 0);

            return () => clearTimeout(timeoutId);
        }
    }, [currentWorkflow, isWorkflowLoading, setWorkflow]);

    useEffect(() => {
        return () => {
            setWorkflow({});
        };
    }, [setWorkflow]);

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
                    <AutomationWorkflowEditorLeftSidebar currentWorkflowId={workflowId!} />
                </div>
            </div>

            <div className="flex w-full flex-col">
                <AutomationWorkflowEditorHeader
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    chatTrigger={
                        workflow.triggers &&
                        workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                    }
                    currentWorkflowId={workflowId!}
                    projectId={projectId}
                    runDisabled={runDisabled}
                    updateWorkflowMutation={updateWorkflowEditorMutation}
                />

                <div className="flex flex-1">
                    <ResizablePanelGroup className="flex-1 bg-surface-main" orientation="vertical">
                        <ResizablePanel className="relative flex" defaultSize={650}>
                            <WorkflowEditorProvider
                                value={{
                                    ConnectionKeys: ConnectionKeys,
                                    cancelWorkflowQueries,
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
                                {workflow.id && (
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
                            {(workflowIsRunning || workflowTestExecution) && (
                                <WorkflowExecutionsTestOutput
                                    onCloseClick={handleWorkflowExecutionsTestOutputCloseClick}
                                    workflowIsRunning={workflowIsRunning}
                                    workflowTestExecution={workflowTestExecution}
                                />
                            )}
                        </ResizablePanel>
                    </ResizablePanelGroup>
                </div>
            </div>
        </div>
    );
};

export default AutomationWorkflow;
