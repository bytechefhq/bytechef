import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ApiConnectorDeleteAlertDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorDeleteAlertDialog';
import ApiConnectorEditDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorEditDialog';
import ApiConnectorEndpointListItem from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorEndpointListItem';
import {ApiConnector} from '@/shared/middleware/graphql';
import {ChevronDownIcon, EllipsisVerticalIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

import useApiConnectorListItem from './hooks/useApiConnectorListItem';

interface ApiConnectorItemProps {
    apiConnector: ApiConnector;
}

const ApiConnectorListItem = ({apiConnector}: ApiConnectorItemProps) => {
    const {
        handleAlertDeleteDialogClick,
        handleOnCheckedChange,
        lastModifiedDate,
        setShowDeleteDialog,
        setShowEditDialog,
        showDeleteDialog,
        showEditDialog,
    } = useApiConnectorListItem({apiConnector});

    const navigate = useNavigate();

    return (
        <Collapsible className="mb-2 w-full rounded border border-border/50 px-3 py-4 hover:bg-surface-neutral-primary-hover">
            <div className="flex items-center justify-between">
                <div className="flex-1">
                    <div className="flex min-h-8 items-center justify-between">
                        <div className="flex w-full items-center justify-between">
                            {apiConnector.description ? (
                                <Tooltip>
                                    <TooltipTrigger>
                                        <div className="space-x-1">
                                            <span className="font-semibold">{apiConnector.title}</span>

                                            <span>-</span>

                                            <span className="text-sm">{apiConnector.name} </span>
                                        </div>
                                    </TooltipTrigger>

                                    <TooltipContent>{apiConnector.description}</TooltipContent>
                                </Tooltip>
                            ) : (
                                <div className="space-x-1">
                                    <span className="font-semibold">{apiConnector.title}</span>

                                    <span>-</span>

                                    <span className="text-sm">{apiConnector.name}</span>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="mt-2 min-h-7 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-content-neutral-secondary">
                                <span className="mr-1">
                                    {apiConnector.endpoints?.length === 1
                                        ? `1 endpoint`
                                        : `${apiConnector.endpoints?.length ?? 0} endpoints`}
                                </span>

                                <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                            </CollapsibleTrigger>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    <div className="flex flex-col items-end gap-y-2">
                        <div className="flex min-h-8 items-center">
                            <Switch checked={apiConnector.enabled ?? false} onCheckedChange={handleOnCheckedChange} />
                        </div>

                        <Tooltip>
                            <TooltipTrigger className="flex min-h-7 items-center text-sm text-content-neutral-secondary">
                                {lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${lastModifiedDate.toLocaleDateString()} ${lastModifiedDate.toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Modified Date</TooltipContent>
                        </Tooltip>
                    </div>

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
                                onClick={() =>
                                    navigate(`/automation/settings/components/api-connectors/${apiConnector.id}/edit`)
                                }
                            >
                                Edit
                            </DropdownMenuItem>

                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit YAML</DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem onClick={() => setShowDeleteDialog(true)} variant="destructive">
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <CollapsibleContent className="mt-4">
                {apiConnector.endpoints && apiConnector.endpoints.length > 0 ? (
                    <ul className="space-y-1 border-t pt-4">
                        {apiConnector.endpoints.map((endpoint) => (
                            <li
                                className="flex items-center justify-between rounded-md p-2 hover:bg-surface-neutral-primary-hover"
                                key={endpoint.id}
                            >
                                <ApiConnectorEndpointListItem
                                    apiConnectorEndpoint={endpoint}
                                    apiConnectorName={apiConnector.name}
                                    specification={apiConnector.specification ?? undefined}
                                />
                            </li>
                        ))}
                    </ul>
                ) : (
                    <div className="border-t pt-4 text-center text-sm text-content-neutral-secondary">
                        No endpoints configured
                    </div>
                )}
            </CollapsibleContent>

            {showDeleteDialog && (
                <ApiConnectorDeleteAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={handleAlertDeleteDialogClick}
                />
            )}

            {showEditDialog && (
                <ApiConnectorEditDialog apiConnector={apiConnector} onClose={() => setShowEditDialog(false)} />
            )}
        </Collapsible>
    );
};

export default ApiConnectorListItem;
