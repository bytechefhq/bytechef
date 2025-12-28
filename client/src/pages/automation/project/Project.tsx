import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import ProjectsLeftSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar';
import {useProject} from '@/pages/automation/project/hooks/useProject';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowEditorProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import WorkflowTestRunLeaveDialog from '@/shared/components/WorkflowTestRunLeaveDialog';
import {useWorkflowTestRunGuard} from '@/shared/hooks/useWorkflowTestRunGuard';
import {WebhookTriggerTestApi} from '@/shared/middleware/automation/configuration';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useShallow} from 'zustand/react/shallow';

const Project = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {projectLeftSidebarOpen} = useProjectsLeftSidebarStore(
        useShallow((state) => ({
            projectLeftSidebarOpen: state.projectLeftSidebarOpen,
        }))
    );
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
        handleProjectClick,
        handleWorkflowExecutionsTestOutputCloseClick,
        invalidateWorkflowQueries,
        projectId,
        projectWorkflowId,
        projects,
        updateClusterElementParameterMutation,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    } = useProject();
    const {runDisabled} = useRun();

    return (
        <div className="flex w-full">
            <WorkflowTestRunLeaveDialog onCancel={cancelLeave} onConfirm={confirmLeave} open={showLeaveDialog} />

            {projectLeftSidebarOpen && projects && (
                <ProjectsLeftSidebar
                    bottomResizablePanelRef={bottomResizablePanelRef}
                    currentWorkflowId={workflow.id!}
                    onProjectClick={handleProjectClick}
                    projectId={projectId}
                />
            )}

            <div className="flex w-full flex-col">
                {projectId && (
                    <ProjectHeader
                        bottomResizablePanelRef={bottomResizablePanelRef}
                        chatTrigger={
                            workflow.triggers &&
                            workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                        }
                        projectId={projectId}
                        projectWorkflowId={projectWorkflowId}
                        runDisabled={runDisabled}
                        updateWorkflowMutation={updateWorkflowMutation}
                    />
                )}

                <div className="flex flex-1">
                    <ResizablePanelGroup className="flex-1 bg-surface-main" direction="vertical">
                        <ResizablePanel className="relative flex" defaultSize={65}>
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
                                {projectId && (
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
                </div>
            </div>
        </div>
    );
};

export default Project;
