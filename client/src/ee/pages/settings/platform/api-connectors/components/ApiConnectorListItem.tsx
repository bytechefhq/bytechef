import Button from '@/components/Button/Button';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
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
import ApiConnectorImportDialog from '@/ee/pages/settings/platform/api-connectors/components/ApiConnectorImportDialog';
import {ApiConnector} from '@/ee/shared/middleware/platform/api-connector';
import {
    useDeleteApiConnectorMutation,
    useEnableApiConnectorMutation,
} from '@/ee/shared/mutations/platform/apiConnector.mutations';
import {ApiConnectorKeys} from '@/ee/shared/queries/platform/apiConnectors.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';

interface ApiConnectorItemProps {
    apiConnector: ApiConnector;
}

const ApiConnectorListItem = ({apiConnector}: ApiConnectorItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteApiConnectorMutation = useDeleteApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiConnectorKeys.apiConnectors,
            });
        },
    });

    const enableApiConnectorMutation = useEnableApiConnectorMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiConnectorKeys.apiConnectors,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (apiConnector.id) {
            deleteApiConnectorMutation.mutate(apiConnector.id);

            setShowDeleteDialog(false);
        }
    };

    const handleOnCheckedChange = (value: boolean) => {
        enableApiConnectorMutation.mutate({
            enable: value,
            id: apiConnector.id!,
        });
    };

    return (
        <div className="w-full rounded-md px-2 py-5 hover:bg-gray-50">
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
                        <Switch checked={apiConnector.enabled} onCheckedChange={handleOnCheckedChange} />

                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {apiConnector.lastModifiedDate ? (
                                    <span className="text-xs">
                                        {`Modified at ${apiConnector.lastModifiedDate?.toLocaleDateString()} ${apiConnector.lastModifiedDate?.toLocaleTimeString()}`}
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

            {showDeleteDialog && (
                <ApiConnectorDeleteAlertDialog
                    onClose={() => setShowDeleteDialog(false)}
                    onDelete={handleAlertDeleteDialogClick}
                />
            )}

            {showEditDialog && (
                <ApiConnectorImportDialog apiConnector={apiConnector} onClose={() => setShowEditDialog(false)} />
            )}
        </div>
    );
};

export default ApiConnectorListItem;
