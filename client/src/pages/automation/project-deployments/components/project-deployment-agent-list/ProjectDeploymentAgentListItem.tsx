import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import AgentDeploymentChannelList, {
    AgentDeploymentSchedule,
} from '@/pages/automation/project-deployments/components/agent-deployment-channel-list/AgentDeploymentChannelList';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {AiAgentDeploymentsQuery} from '@/shared/middleware/graphql';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {ProjectDeploymentKeys} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {BotIcon} from 'lucide-react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

export type ProjectDeploymentAgentType = AiAgentDeploymentsQuery['aiAgentDeployments'][number];

interface ProjectDeploymentAgentListItemAgentI {
    id: string;
    projectId: string;
}

interface ProjectDeploymentAgentListItemProps {
    agent?: ProjectDeploymentAgentListItemAgentI;
    agentDeployment: ProjectDeploymentAgentType;
    projectDeploymentWorkflow?: ProjectDeploymentWorkflow;
}

const ProjectDeploymentAgentListItem = ({
    agent,
    agentDeployment,
    projectDeploymentWorkflow,
}: ProjectDeploymentAgentListItemProps) => {
    const queryClient = useQueryClient();

    const enableProjectDeploymentWorkflowMutation = useEnableProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ProjectDeploymentKeys.projectDeployments});
            queryClient.invalidateQueries({queryKey: ['aiAgentDeployments']});
        },
    });

    const {agentTitle} = agentDeployment;
    const enabled = projectDeploymentWorkflow?.enabled ?? false;
    const lastExecutionDate = projectDeploymentWorkflow?.lastExecutionDate;
    const lastExecutionStatus = projectDeploymentWorkflow?.lastExecutionStatus;

    const title = (
        <>
            <BotIcon className="size-4 shrink-0 text-content-neutral-secondary" />

            <span className={twMerge('truncate', !enabled && 'text-content-neutral-secondary')}>{agentTitle}</span>
        </>
    );

    return (
        <li className="flex items-center justify-between rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <div className="flex min-w-0 flex-1 items-center">
                {agent ? (
                    <Link
                        aria-label={`Link to agent ${agentTitle}`}
                        className="flex w-full max-w-80 min-w-0 shrink items-center gap-2 pr-1 text-sm font-semibold"
                        to={getAgentPath(agent)}
                    >
                        {title}
                    </Link>
                ) : (
                    <div className="flex w-full max-w-80 min-w-0 shrink items-center gap-2 pr-1 text-sm font-semibold">
                        {title}
                    </div>
                )}

                <div className="ml-6 hidden min-w-0 items-center sm:flex">
                    <AgentDeploymentChannelList
                        projectDeploymentId={agentDeployment.id}
                        title={agentTitle}
                        workflows={agentDeployment.workflows}
                    />
                </div>
            </div>

            <div className="flex shrink-0 items-center gap-x-4">
                <AgentDeploymentSchedule workflows={agentDeployment.workflows} />

                {lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger
                            className={twMerge(
                                'flex items-center text-content-neutral-secondary',
                                lastExecutionStatus === 'FAILED' && 'text-content-destructive-primary'
                            )}
                        >
                            <span className="pr-1 text-xs capitalize">
                                {lastExecutionStatus?.toLocaleLowerCase() || ''}
                            </span>

                            <span className="text-xs">
                                {`at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                <div className="flex items-center gap-x-6">
                    <div className="min-w-[36px]" />

                    {projectDeploymentWorkflow && (
                        <div className="relative flex items-center">
                            {enableProjectDeploymentWorkflowMutation.isPending && (
                                <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                            )}

                            <Switch
                                aria-label={`Enable agent ${agentTitle}`}
                                checked={enabled}
                                disabled={enableProjectDeploymentWorkflowMutation.isPending}
                                onCheckedChange={(value) =>
                                    enableProjectDeploymentWorkflowMutation.mutate({
                                        enable: value,
                                        id: +agentDeployment.id,
                                        workflowId: projectDeploymentWorkflow.workflowId!,
                                    })
                                }
                            />
                        </div>
                    )}
                </div>

                <div className="size-9 shrink-0" />
            </div>
        </li>
    );
};

export default ProjectDeploymentAgentListItem;
