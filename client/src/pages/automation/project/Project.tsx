import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import ProjectsLeftSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar';
import {useProject} from '@/pages/automation/project/hooks/useProject';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useRun} from '@/pages/platform/workflow-editor/hooks/useRun';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowNodeParameterMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ConnectionReactQueryProvider} from '@/shared/components/connection/providers/connectionReactQueryProvider';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useEffect} from 'react';

const Project = () => {
    const {projectLeftSidebarOpen, setProjectLeftSidebarOpen} = useProjectsLeftSidebarStore();
    const {setRightSidebarOpen} = useRightSidebarStore();
    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();
    const {workflow} = useWorkflowDataStore();

    const {
        bottomResizablePanelRef,
        deleteWorkflowNodeParameterMutation,
        handleProjectClick,
        handleWorkflowExecutionsTestOutputCloseClick,
        projectId,
        projectWorkflowId,
        projects,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    } = useProject();

    const {runDisabled} = useRun();

    useEffect(() => {
        return () => {
            setProjectLeftSidebarOpen(false);

            setRightSidebarOpen(false);
        };
    }, [setProjectLeftSidebarOpen, setRightSidebarOpen]);

    return (
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
                        {projectLeftSidebarOpen && projects && (
                            <ProjectsLeftSidebar
                                bottomResizablePanelRef={bottomResizablePanelRef}
                                currentWorkflowId={workflow.id!}
                                onProjectClick={handleProjectClick}
                                projectId={projectId}
                                updateWorkflowMutation={updateWorkflowMutation}
                            />
                        )}

                        <ConnectionReactQueryProvider
                            value={{
                                ConnectionKeys: ConnectionKeys,
                                useCreateConnectionMutation: useCreateConnectionMutation,
                                useGetConnectionTagsQuery: useGetConnectionTagsQuery,
                                useGetConnectionsQuery,
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
                                    {projectId && (
                                        <WorkflowEditorLayout
                                            parentId={projectId}
                                            parentType="PROJECT"
                                            runDisabled={runDisabled}
                                            showWorkflowInputs={true}
                                        />
                                    )}
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
            </div>
        </div>
    );
};

export default Project;
