import Button from '@/components/Button/Button';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ApiConnectorDeleteAlertDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorDeleteAlertDialog';
import ApiConnectorEditDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorEditDialog';
import {ApiConnector, useDeleteApiConnectorMutation, useEnableApiConnectorMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface ApiConnectorItemProps {
    apiConnector: ApiConnector;
}

const HTTP_METHOD_COLORS: Record<string, string> = {
    DELETE: 'bg-red-100 text-red-700',
    GET: 'bg-green-100 text-green-700',
    PATCH: 'bg-orange-100 text-orange-700',
    POST: 'bg-blue-100 text-blue-700',
    PUT: 'bg-yellow-100 text-yellow-700',
};

const getHttpMethodBadgeColor = (method?: string | null): string => {
    return (method && HTTP_METHOD_COLORS[method]) || 'bg-gray-100 text-gray-700';
};

const ApiConnectorListItem = ({apiConnector}: ApiConnectorItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteApiConnectorMutation = useDeleteApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['apiConnectors'],
            });
        },
    });

    const enableApiConnectorMutation = useEnableApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['apiConnectors'],
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (apiConnector.id) {
            deleteApiConnectorMutation.mutate({id: apiConnector.id});

            setShowDeleteDialog(false);
        }
    };

    const handleOnCheckedChange = (value: boolean) => {
        enableApiConnectorMutation.mutate({
            enable: value,
            id: apiConnector.id,
        });
    };

    return (
        <Collapsible className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
            <div className="flex items-center justify-between">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
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

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-gray-700">
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
                    <div className="flex flex-col items-end gap-y-4">
                        <Switch checked={apiConnector.enabled ?? false} onCheckedChange={handleOnCheckedChange} />

                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {apiConnector.lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${new Date(apiConnector.lastModifiedDate).toLocaleDateString()} ${new Date(apiConnector.lastModifiedDate).toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Created Date</TooltipContent>
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
                            <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            <CollapsibleContent className="mt-4">
                {apiConnector.endpoints && apiConnector.endpoints.length > 0 ? (
                    <div className="space-y-2 border-t pt-4">
                        {apiConnector.endpoints.map((endpoint) => (
                            <div
                                className="flex items-center justify-between rounded-md bg-gray-50 px-3 py-2"
                                key={endpoint.id}
                            >
                                <div className="flex items-center gap-3">
                                    <span
                                        className={`rounded px-2 py-0.5 text-xs font-medium ${getHttpMethodBadgeColor(endpoint.httpMethod)}`}
                                    >
                                        {endpoint.httpMethod}
                                    </span>

                                    <span className="text-sm font-medium">{endpoint.name}</span>

                                    <span className="text-sm text-gray-500">{endpoint.path}</span>
                                </div>

                                {endpoint.description && (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="max-w-xs truncate text-xs text-gray-400">
                                                {endpoint.description}
                                            </span>
                                        </TooltipTrigger>

                                        <TooltipContent>{endpoint.description}</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="border-t pt-4 text-center text-sm text-gray-500">No endpoints configured</div>
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
