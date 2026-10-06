import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useEnableIntegrationInstanceWorkflowMutation} from '@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {IntegrationInstanceKeys} from '@/ee/shared/queries/embedded/integrationInstances.queries';
import {ConnectedUserMcpServerWorkflow} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

const ConnectedUserMcpServerListItemWorkflowRow = ({
    connectedUserId,
    workflow,
}: {
    connectedUserId: number;
    workflow: ConnectedUserMcpServerWorkflow;
}) => {
    const lastExecutionDate = workflow.lastExecutionDate ? new Date(Date.parse(workflow.lastExecutionDate)) : undefined;

    const queryClient = useQueryClient();

    const enableIntegrationInstanceWorkflowMutation = useEnableIntegrationInstanceWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
            void queryClient.invalidateQueries({queryKey: ConnectedUserKeys.connectedUser(connectedUserId)});
            void queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstance(Number(workflow.integrationInstanceId)),
            });
        },
    });

    return (
        <li className="flex items-center gap-2 py-0.5">
            <div className="flex min-w-0 flex-1 flex-col">
                <span className="truncate text-sm font-medium">{workflow.name}</span>

                {workflow.description && (
                    <span className="truncate text-xs text-muted-foreground">{workflow.description}</span>
                )}
            </div>

            <Tooltip>
                <TooltipTrigger className="shrink-0 text-xs text-content-neutral-secondary">
                    {lastExecutionDate
                        ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                        : 'No executions'}
                </TooltipTrigger>

                <TooltipContent>Last Execution Date</TooltipContent>
            </Tooltip>

            <div className="relative mr-11 flex shrink-0 items-center">
                {enableIntegrationInstanceWorkflowMutation.isPending && (
                    <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                )}

                <Switch
                    checked={workflow.enabled}
                    onCheckedChange={(value) => {
                        enableIntegrationInstanceWorkflowMutation.mutate({
                            enable: value,
                            id: Number(workflow.integrationInstanceId),
                            workflowId: workflow.workflowId,
                        });
                    }}
                />
            </div>
        </li>
    );
};

export default ConnectedUserMcpServerListItemWorkflowRow;
