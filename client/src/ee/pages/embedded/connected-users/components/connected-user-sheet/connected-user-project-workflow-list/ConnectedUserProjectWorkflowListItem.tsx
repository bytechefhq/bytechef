import Badge from '@/components/Badge/Badge';
import LoadingIcon from '@/components/LoadingIcon';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ConnectedUserProjectWorkflow, useEnableConnectedUserProjectWorkflowMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

const ConnectedUserProjectWorkflowListItem = ({
    connectedUserProjectWorkflow,
}: {
    connectedUserProjectWorkflow: ConnectedUserProjectWorkflow;
}) => {
    const queryClient = useQueryClient();

    const enableConnectedUserProjectWorkflowMutation = useEnableConnectedUserProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['connectedUserProjects']});
        },
    });

    const lastExecutionDate = connectedUserProjectWorkflow.lastExecutionDate
        ? new Date(Date.parse(connectedUserProjectWorkflow.lastExecutionDate))
        : undefined;

    const isDraft = connectedUserProjectWorkflow.workflowVersion == null;

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-3.5 hover:bg-gray-50">
            <div className="flex flex-1 cursor-pointer items-center">
                <span className="w-80 text-sm font-semibold">{connectedUserProjectWorkflow.workflow.label}</span>

                <div className="ml-6 flex space-x-1"></div>
            </div>

            <div className="flex items-center gap-x-4 text-content-neutral-secondary">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Badge
                            label={isDraft ? 'V1 DRAFT' : `V${connectedUserProjectWorkflow.workflowVersion}`}
                            styleType="secondary-filled"
                            weight="semibold"
                        />
                    </TooltipTrigger>

                    <TooltipContent>The workflow version</TooltipContent>
                </Tooltip>

                <div className="flex w-48 items-center justify-end">
                    {connectedUserProjectWorkflow?.lastExecutionDate ? (
                        <Tooltip>
                            <TooltipTrigger className="flex items-center justify-end text-sm">
                                <span className="text-xs">
                                    {`Executed at ${lastExecutionDate?.toLocaleDateString()} ${lastExecutionDate?.toLocaleTimeString()}`}
                                </span>
                            </TooltipTrigger>

                            <TooltipContent>Last Execution Date</TooltipContent>
                        </Tooltip>
                    ) : (
                        <span className="text-xs">No executions</span>
                    )}
                </div>

                <div className="relative flex items-center">
                    {enableConnectedUserProjectWorkflowMutation.isPending && (
                        <LoadingIcon className="absolute left-[-15px] top-[3px]" />
                    )}

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Switch
                                checked={connectedUserProjectWorkflow.enabled}
                                disabled={isDraft}
                                onCheckedChange={(value) => {
                                    enableConnectedUserProjectWorkflowMutation.mutate({
                                        enable: value,
                                        id: connectedUserProjectWorkflow.id,
                                    });
                                }}
                            />
                        </TooltipTrigger>

                        <TooltipContent>
                            {isDraft ? 'Publish the workflow before enabling it' : 'Enable workflow'}
                        </TooltipContent>
                    </Tooltip>
                </div>
            </div>
        </li>
    );
};

export default ConnectedUserProjectWorkflowListItem;
