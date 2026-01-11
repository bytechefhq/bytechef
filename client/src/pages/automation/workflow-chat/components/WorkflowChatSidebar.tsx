import {Skeleton} from '@/components/ui/skeleton';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {LeftSidebarNav, LeftSidebarNavItem} from '@/shared/layout/LeftSidebarNav';
import {useWorkflowChatWorkspaceProjectDeploymentsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {MessageCircleMoreIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useParams} from 'react-router-dom';

interface WorkflowWithDeploymentProps {
    workflowId: string;
    workflowLabel: string;
    projectDeploymentId: number;
    enabled: boolean;
    workflowExecutionId?: string | null;
}

const hasHostedChatTrigger = (
    triggers?: Array<{type: string; parameters?: {mode?: number} | Record<string, unknown>}>
): boolean => {
    return (
        !!triggers &&
        triggers.findIndex((trigger) => trigger.type?.includes('chat/')) !== -1 &&
        ((triggers?.[0]?.parameters as {mode?: number})?.mode ?? 1) === 1
    );
};

const WorkflowChatSidebar = () => {
    const {workflowExecutionId} = useParams();
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const reset = useWorkflowChatStore((state) => state.reset);

    const {data, isLoading} = useWorkflowChatWorkspaceProjectDeploymentsQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const workflowsByProject: Map<string, {projectName: string; workflows: WorkflowWithDeploymentProps[]}> =
        useMemo(() => {
            if (isLoading || !data?.workspaceProjectDeployments) {
                return new Map();
            }

            const result = new Map<string, {projectName: string; workflows: WorkflowWithDeploymentProps[]}>();

            data.workspaceProjectDeployments.forEach((projectDeployment) => {
                const projectId = projectDeployment.project.id;
                const projectName = projectDeployment.project?.name || 'Untitled Project';

                projectDeployment.projectDeploymentWorkflows.forEach((projectDeploymentWorkflow) => {
                    const workflowId = projectDeploymentWorkflow.projectWorkflow.workflow.id;
                    const projectWorkflow = projectDeploymentWorkflow.projectWorkflow;
                    const triggers = projectWorkflow?.workflow?.triggers;

                    if (
                        workflowId &&
                        projectWorkflow &&
                        projectDeploymentWorkflow.enabled &&
                        projectDeploymentWorkflow.staticWebhookUrl &&
                        hasHostedChatTrigger(triggers)
                    ) {
                        if (!result.has(projectId)) {
                            result.set(projectId, {projectName, workflows: []});
                        }

                        result.get(projectId)!.workflows.push({
                            enabled: projectDeploymentWorkflow.enabled,
                            projectDeploymentId: Number(projectDeployment.id),
                            workflowExecutionId: projectDeploymentWorkflow.workflowExecutionId,
                            workflowId,
                            workflowLabel: projectWorkflow.workflow.label || 'Untitled Workflow',
                        });
                    }
                });
            });

            return result;
        }, [isLoading, data]);

    if (isLoading) {
        return (
            <div className="space-y-2 p-4">
                {[1, 2, 3].map((value) => (
                    <div className="flex items-center space-x-2" key={value}>
                        <Skeleton className="size-5" />

                        <Skeleton className="h-4 flex-1" />
                    </div>
                ))}
            </div>
        );
    }

    return (
        <>
            {workflowsByProject.size === 0 ? (
                <div className="mb-4 px-2">
                    <span className="px-3 text-xs">No chat workflows found</span>
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
                                            icon={<MessageCircleMoreIcon className="mr-1 size-4" />}
                                            item={{
                                                current: isActive,
                                                id: `${workflowData.projectDeploymentId}-${workflowData.workflowId}`,
                                                name: workflowData.workflowLabel,
                                                onItemClick: () => reset(),
                                            }}
                                            key={`${workflowData.projectDeploymentId}-${workflowData.workflowId}`}
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
