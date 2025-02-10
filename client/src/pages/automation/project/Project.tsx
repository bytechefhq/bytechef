import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import ProjectHeader from '@/pages/automation/project/components/project-header/ProjectHeader';
import ProjectsSidebar from '@/pages/automation/project/components/projects-sidebar/ProjectsSidebar';
import ProjectsSidebarHeader from '@/pages/automation/project/components/projects-sidebar/ProjectsSidebarHeader';
import {useProject} from '@/pages/automation/project/hooks/useProject';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import WorkflowEditorLayout from '@/pages/platform/workflow-editor/WorkflowEditorLayout';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import {useWorkflowLayout} from '@/pages/platform/workflow-editor/hooks/useWorkflowLayout';
import {WorkflowMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {WorkflowNodeParameterMutationProvider} from '@/pages/platform/workflow-editor/providers/workflowNodeParameterMutationProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {ConnectionReactQueryProvider} from '@/shared/components/connection/providers/connectionReactQueryProvider';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useCreateConnectionMutation} from '@/shared/mutations/automation/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {useParams} from 'react-router-dom';

const Project = () => {
    const {leftSidebarOpen} = useProjectsLeftSidebarStore();
    const {workflowIsRunning, workflowTestExecution} = useWorkflowEditorStore();
    const {workflow} = useWorkflowDataStore();

    const {projectId, projectWorkflowId} = useParams();

    const {
        bottomResizablePanelRef,
        deleteWorkflowNodeParameterMutation,
        handleWorkflowExecutionsTestOutputCloseClick,
        updateWorkflowEditorMutation,
        updateWorkflowMutation,
        updateWorkflowNodeParameterMutation,
        useGetConnectionsQuery,
    } = useProject({
        projectId: parseInt(projectId!),
        projectWorkflowId: parseInt(projectWorkflowId!),
    });

    const {runDisabled} = useWorkflowLayout();

    return (
        <>
            <LayoutContainer
                className="bg-muted/50"
                leftSidebarBody={<ProjectsSidebar projectId={+projectId!} />}
                leftSidebarClass="bg-background"
                leftSidebarHeader={<Header right={<ProjectsSidebarHeader />} title="Projects" />}
                leftSidebarOpen={leftSidebarOpen}
                leftSidebarWidth="96"
                topHeader={
                    projectId && (
                        <ProjectHeader
                            bottomResizablePanelRef={bottomResizablePanelRef}
                            chatTrigger={
                                workflow.triggers &&
                                workflow.triggers.findIndex((trigger) => trigger.type.includes('chat/')) !== -1
                            }
                            projectId={parseInt(projectId)}
                            projectWorkflowId={parseInt(projectWorkflowId!)}
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

export default Project;
