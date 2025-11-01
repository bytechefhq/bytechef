import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {Tag} from '@/shared/middleware/automation/configuration';
import {ConnectedUserProject} from '@/shared/middleware/graphql';
import {ChevronDownIcon} from 'lucide-react';

interface ConnectedUserProjectListItemProps {
    connectedUserProject: ConnectedUserProject;
    remainingTags?: Tag[];
}

const ConnectedUserProjectListItem = ({connectedUserProject}: ConnectedUserProjectListItemProps) => {
    const lastExecutionDate = connectedUserProject.lastExecutionDate
        ? new Date(Date.parse(connectedUserProject.lastExecutionDate))
        : undefined;

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center gap-2">
                                <span className="text-base font-semibold">
                                    User {connectedUserProject.connectedUser.externalId}
                                </span>
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-muted-foreground">
                                    <span className="mr-1">
                                        {connectedUserProject.connectedUserProjectWorkflows?.length === 1
                                            ? `1 workflow`
                                            : `${connectedUserProject.connectedUserProjectWorkflows?.length} workflows`}
                                    </span>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex min-w-52 flex-col items-end">
                            <div className="flex items-center"></div>

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {lastExecutionDate ? (
                                        <span className="text-xs">
                                            {`Executed at ${lastExecutionDate?.toLocaleDateString()} ${lastExecutionDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">No executions</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Execution Date</TooltipContent>
                            </Tooltip>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
};

export default ConnectedUserProjectListItem;
