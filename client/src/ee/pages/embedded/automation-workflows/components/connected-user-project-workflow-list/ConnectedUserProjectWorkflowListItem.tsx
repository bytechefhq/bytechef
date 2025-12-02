import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ConnectedUserProjectWorkflow} from '@/shared/middleware/graphql';

const ConnectedUserProjectWorkflowListItem = ({
    connectedUserProjectWorkflow,
}: {
    connectedUserProjectWorkflow: ConnectedUserProjectWorkflow;
}) => {
    const lastExecutionDate = connectedUserProjectWorkflow.lastExecutionDate
        ? new Date(Date.parse(connectedUserProjectWorkflow.lastExecutionDate))
        : undefined;

    return (
        <li className="flex items-center justify-between rounded-md px-2 py-3.5 hover:bg-gray-50">
            <div className="flex flex-1 cursor-pointer items-center">
                <span className="w-80 text-sm font-semibold">{connectedUserProjectWorkflow.workflow.label}</span>

                <div className="ml-6 flex space-x-1"></div>
            </div>

            <div className="flex items-center gap-x-4 text-gray-500">
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Badge
                            label={`V${connectedUserProjectWorkflow.workflowVersion}`}
                            styleType="secondary-filled"
                            weight="semibold"
                        />
                    </TooltipTrigger>

                    <TooltipContent>The workflow version</TooltipContent>
                </Tooltip>

                <div className="w-48">
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
            </div>
        </li>
    );
};

export default ConnectedUserProjectWorkflowListItem;
