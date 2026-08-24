import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import ProjectsLeftSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar';
import {useProject} from '@/pages/automation/project/hooks/useProject';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRegisterWorkflowEditorCommands} from '@/pages/platform/workflow-editor/hooks/useRegisterWorkflowEditorCommands';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {useLoadWorkspacePermissions} from '@/shared/hooks/useLoadWorkspacePermissions';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {Project as ProjectType, WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useGetProjectQuery} from '@/shared/queries/automation/projects.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useLoaderData} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const Project = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const connectionTagsQueryResult = useGetConnectionTagsQuery(currentWorkspaceId!);
    const {workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            workflow: state.workflow,
        }))
    );

    const showBottomPanel = useWorkflowEditorStore((state) => state.showBottomPanel);
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);

    const {cancelLeave, confirmLeave, showLeaveDialog, workflowIsRunning, workflowTestExecution} =
        useWorkflowTestRunGuard(workflow.id, currentEnvironmentId);

    const {
        bottomResizablePanelRef,
        cancelWorkflowQueries,
        deleteClusterElementParameterMutation,
        deleteWorkflowNodeParameterMutation,
        handleEditSubflowClick,
        handleProjectClick,
        handleWorkflowExecutionsTestOutputCloseClick,
        invalidateWorkflowQueries,
        projectId,
        projectLeftSidebarOpen,
        projectWorkflowId,
        sidebarLoaded,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    } = useProject();

    const {data: project} = useGetProjectQuery(projectId, useLoaderData() as ProjectType);

    useLoadWorkspacePermissions(currentWorkspaceId);
    useLoadWorkspaceScopes(currentWorkspaceId);
    useRegisterWorkflowEditorCommands();

    const {runDisabled} = useRun();

    const copilotLayoutShifted = useCopilotLayoutShifted();

    return (
        <div className="flex w-full">
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            <div className="h-full shrink-0 overflow-hidden">
                <div
                    className={twMerge(
                        'h-full w-[355px] transition-[margin-left,opacity] duration-300 ease-out',
                        projectLeftSidebarOpen ? 'ml-0 opacity-100' : 'ml-[-355px] opacity-0'
                    )}
                >
                    {sidebarLoaded && (
                        <ProjectsLeftSidebar
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            currentWorkflowId={workflow.id!}
                            onProjectClick={handleProjectClick}
                            projectId={projectId}
                        />
                    )}
                </div>
            </div>

            <div className="flex w-full flex-col">
                {projectId && (
                    <ProjectHeader
                        bottomResizablePanelRef={bottomResizablePanelRef}
                        chatTrigger={
                            workflow.triggers &&
                            workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                        }
                        codeWorkflow={project?.codeWorkflow}
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
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
                                    codeWorkflow: project?.codeWorkflow,
                                    codeWorkflowLanguage: project?.codeWorkflowLanguage,
                                    connectionTagsQueryKey: ConnectionKeys.connectionTags(currentWorkspaceId!),
                                    deleteClusterElementParameterMutation,
                                    deleteWorkflowNodeParameterMutation,
                                    invalidateWorkflowQueries,
                                    updateClusterElementParameterMutation,
                                    updateWorkflowMutation: updateWorkflowEditorMutation,
                                    updateWorkflowNodeParameterMutation,
                                    useCreateConnectionMutation: useCreateConnectionMutation,
                                    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                                    useGetConnectionTagsQuery: () => connectionTagsQueryResult,
                                    useGetConnectionsQuery,
                                    webhookTriggerTestApi: new WebhookTriggerTestApi(),
                                }}
                            >
                                {projectId && (
                                    <WorkflowEditorLayout
                                        enableUndoRedo={true}
                                        leftSidebarOpen={projectLeftSidebarOpen}
                                        onEditSubflowClick={handleEditSubflowClick}
                                        runDisabled={runDisabled}
                                        showWorkflowInputs={true}
                                        workflowReferenceId={projectWorkflowId}
                                    />
                                )}
                            </WorkflowEditorProvider>
                        </ResizablePanel>

                        <ResizableHandle className="bg-surface-neutral-secondary" withHandle />

                        <ResizablePanel
                            className={twMerge(
                                'bg-surface-main px-3 py-3',
                                projectLeftSidebarOpen && 'pl-0',
                                copilotLayoutShifted && 'pr-0'
                            )}
                            defaultSize={0}
                            panelRef={bottomResizablePanelRef}
                        >
                            {(showBottomPanel ||
                                workflowIsRunning ||
                                workflowTestExecution ||
                                workflowTestChatPanelOpen) && (
                                <WorkflowExecutionsTestOutput
                                    onCloseClick={handleWorkflowExecutionsTestOutputCloseClick}
                                    onEditSubflowClick={handleEditSubflowClick}
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

export default Project;
