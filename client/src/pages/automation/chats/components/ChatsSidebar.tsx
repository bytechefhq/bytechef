import {Skeleton} from '@/components/ui/skeleton';
import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useWorkspaceChatWorkflowsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';
import {useParams} from 'react-router-dom';

const ChatsSidebarSkeleton = () => (
    <>
        {Array.from({length: 2}).map((_, groupIndex) => (
            <div className="mb-6 px-4" key={groupIndex}>
                <Skeleton className="my-1 h-4 w-28" />

                <div className="mt-2 flex flex-col gap-2">
                    {Array.from({length: 6}).map((_, itemIndex) => (
                        <Skeleton className="h-6 w-full" key={itemIndex} />
                    ))}
                </div>
            </div>
        ))}
    </>
);

interface ProjectChatGroupI {
    projectName: string;
    workflows: Array<{
        projectDeploymentId: string;
        workflowExecutionId: string;
        workflowLabel: string;
    }>;
}

const ChatsSidebar = () => {
    const {workflowExecutionId} = useParams();

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const isRunning = useChatsStore((state) => state.isRunning);

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
        return <ChatsSidebarSkeleton />;
    }

    return (
        <>
            {workflowsByProject.size === 0 ? (
                <div className="mb-4 px-2">
                    <span className="px-3 text-xs">No chats found</span>
                </div>
            ) : (
                Array.from(workflowsByProject.entries()).map(([projectId, {projectName, workflows}]) => (
                    <LeftSidebarNav
                        body={
                            <>
                                {workflows.map((workflowData) => {
                                    const chatUrl = `/automation/chats/${workflowData.workflowExecutionId}`;
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

export default ChatsSidebar;
