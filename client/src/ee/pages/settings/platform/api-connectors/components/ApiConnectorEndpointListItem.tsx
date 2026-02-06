import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ApiConnectorEndpoint} from '@/shared/middleware/graphql';
import {CalendarIcon, EllipsisVerticalIcon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

import useApiConnectorEndpointListItem from '../hooks/useApiConnectorEndpointListItem';

interface ApiConnectorEndpointListItemProps {
    apiConnectorEndpoint: ApiConnectorEndpoint;
    apiConnectorName: string;
    specification?: string;
}

const ApiConnectorEndpointListItem = ({
    apiConnectorEndpoint,
    apiConnectorName,
    specification,
}: ApiConnectorEndpointListItemProps) => {
    const {handleClick, icon, lastExecutionDate, method, setShowEditWorkflowDialog, showEditWorkflowDialog, textColor} =
        useApiConnectorEndpointListItem({apiConnectorEndpoint, apiConnectorName, specification});

    return (
        <>
            <button className="flex flex-1 items-center text-left" onClick={handleClick} type="button">
                <div className="flex flex-1 items-center">
                    {method && (
                        <Badge
                            className={twMerge('mr-4 w-20', textColor)}
                            icon={icon}
                            label={method}
                            styleType="outline-outline"
                            weight="semibold"
                        />
                    )}

                    <div className="w-2/6 text-sm font-semibold">{apiConnectorEndpoint.name}</div>

                    <div className="text-xs text-gray-500">{apiConnectorEndpoint.path}</div>
                </div>
            </button>

            <div className="flex items-center justify-end gap-x-6">
                {lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <CalendarIcon aria-hidden="true" className="mr-0.5 size-3.5 shrink-0 text-gray-400" />

                            <span className="text-xs">
                                {`Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            disabled
                            onClick={() => setShowEditWorkflowDialog(true)}
                            title="Edit workflow (coming soon)"
                        >
                            Edit
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showEditWorkflowDialog && (
                // TODO: Implement edit workflow dialog
                // <ProjectDeploymentEditWorkflowDialog
                //     onClose={() => setShowEditWorkflowDialog(false)}
                //     projectDeploymentEnabled={projectDeploymentEnabled}
                //     projectDeploymentWorkflow={projectDeploymentWorkflow}
                //     workflow={workflow}
                // />
                <></>
            )}
        </>
    );
};

export default ApiConnectorEndpointListItem;
