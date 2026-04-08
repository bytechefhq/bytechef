import {Skeleton} from '@/components/ui/skeleton';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';
import {useParams} from 'react-router-dom';

interface ProjectChatGroupI {
    projectName: string;
    workflows: Array<{
        projectDeploymentId: string;
        workflowExecutionId: string;
        workflowLabel: string;
    }>;
}

const WorkflowChatSidebar = () => {
    const {workflowExecutionId} = useParams();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const isRunning = useWorkflowChatStore((state) => state.isRunning);

    const {data, isLoading} = useWorkspaceChatWorkflowsQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const workflowsByProject: Map<string, ProjectChatGroupI> = useMemo(() => {
        const result = new Map<string, ProjectChatGroupI>();

        if (isLoading || !data?.workspaceChatWorkflows) {
            return result;
        }

        data.workspaceChatWorkflows.forEach((chatWorkflow) => {
            const group = result.get(chatWorkflow.projectId) ?? {
                projectName: chatWorkflow.projectName,
                workflows: [],
            };

            group.workflows.push({
                projectDeploymentId: chatWorkflow.projectDeploymentId,
                workflowExecutionId: chatWorkflow.workflowExecutionId,
                workflowLabel: chatWorkflow.workflowLabel,
            });

            result.set(chatWorkflow.projectId, group);
        });

        return result;
    }, [isLoading, data]);

    if (isLoading) {
        return (
            <div className="space-y-2 p-4">
                <Skeleton className="h-4 w-1/2" />

                {[1, 2, 3].map((value) => (
                    <Skeleton className="h-6" key={value} />
                ))}
            </div>
        );
    }

    return (
        <>
            {workflowsByProject.size === 0 && data ? (
                <div className="mb-4 px-2">
                    <span className="px-3 text-xs">No chats found</span>
                </div>
            ) : (
                Array.from(workflowsByProject.entries()).map(([projectId, {projectName, workflows}]) => (
                    <LeftSidebarNav
                        body={
                            <>
                                {workflows.map((workflowData) => {
                                    const chatUrl = `/automation/chat/${workflowData.workflowExecutionId}`;
                                    const isActive = workflowExecutionId === workflowData.workflowExecutionId;

                                    return (
                                        <LeftSidebarNavItem
                                            disabled={isRunning && !isActive}
                                            item={{
                                                current: isActive,
                                                id: `${workflowData.projectDeploymentId}-${workflowData.workflowExecutionId}`,
                                                name: workflowData.workflowLabel,
                                            }}
                                            key={`${workflowData.projectDeploymentId}-${workflowData.workflowExecutionId}`}
                                            toLink={chatUrl}
                                        />
                                    );
                                })}
                            </>
                        }
                        key={projectId}
                        title={projectName}
                    />
                ))
            )}
        </>
    );
};

export default WorkflowChatSidebar;
