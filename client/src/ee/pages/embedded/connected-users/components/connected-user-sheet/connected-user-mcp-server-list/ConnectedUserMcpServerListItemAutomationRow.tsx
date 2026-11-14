import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ConnectedUserMcpServerAutomation,
    useEnableConnectedUserMcpAutomationMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {twMerge} from 'tailwind-merge';

const ConnectedUserMcpServerListItemAutomationRow = ({
    automation,
    connectedUserId,
}: {
    automation: ConnectedUserMcpServerAutomation;
    connectedUserId: number;
}) => {
    const lastExecutionDate = automation.lastExecutionDate
        ? new Date(Date.parse(automation.lastExecutionDate))
        : undefined;

    const queryClient = useQueryClient();

    const enableConnectedUserMcpAutomationMutation = useEnableConnectedUserMcpAutomationMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['connectedUserMcpServers']});
        },
    });

    const switchDisabled = !automation.available && !automation.enabled;

    return (
        <li>
            <div
                className={twMerge(
                    '-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover',
                    !automation.available && 'opacity-50'
                )}
            >
                <div className="flex min-w-0 flex-1 items-center gap-2">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="max-w-full truncate text-left text-sm font-medium">{automation.name}</span>
                        </TooltipTrigger>

                        <TooltipContent align="start">
                            {automation.available ? automation.name : 'Not available to this user'}
                        </TooltipContent>
                    </Tooltip>

                    <span className="truncate text-xs text-content-neutral-secondary">{automation.automationName}</span>
                </div>

                <Tooltip>
                    <TooltipTrigger className="shrink-0 text-xs text-content-neutral-secondary">
                        {lastExecutionDate
                            ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                            : 'No executions'}
                    </TooltipTrigger>

                    <TooltipContent side="left">Last Execution Date</TooltipContent>
                </Tooltip>

                <div className="relative mr-11 flex shrink-0 items-center">
                    {enableConnectedUserMcpAutomationMutation.isPending && (
                        <LoadingIcon className="absolute top-[3px] left-[-15px]" />
                    )}

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <span className="flex items-center">
                                <Switch
                                    aria-label={`Enable ${automation.name}`}
                                    checked={automation.enabled}
                                    disabled={switchDisabled}
                                    onCheckedChange={(value) => {
                                        enableConnectedUserMcpAutomationMutation.mutate({
                                            connectedUserId: connectedUserId.toString(),
                                            enable: value,
                                            id: automation.id,
                                        });
                                    }}
                                />
                            </span>
                        </TooltipTrigger>

                        {switchDisabled && <TooltipContent side="left">Not available to this user</TooltipContent>}
                    </Tooltip>
                </div>
            </div>
        </li>
    );
};

export default ConnectedUserMcpServerListItemAutomationRow;
