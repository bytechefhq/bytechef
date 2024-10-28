import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import TagList from '@/components/TagList';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
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
import {useDeleteApiCollectionMutation} from '@/ee/mutations/apiCollections.mutations';
import ApiCollectionDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionDialog';
import ApiCollectionEndpointDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog';
import {useApiCollectionsEnabledStore} from '@/ee/pages/automation/api-platform/api-collections/stores/useApiCollectionsEnabledStore';
import {ApiCollectionKeys} from '@/ee/queries/apiCollections.queries';
import {ApiCollection} from '@/middleware/automation/api-platform';
import {useEnableProjectInstanceMutation} from '@/shared/mutations/automation/projectInstances.mutations';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {UseMutationResult, useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';

interface ApiCollectionListItemProps {
    apiCollection: ApiCollection;
}

const ApiCollectionListItem = ({apiCollection}: ApiCollectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showApiEndpointDialog, setShowApiEndpointDialog] = useState(false);

    const setApiCollectionEnabled = useApiCollectionsEnabledStore(
        ({setApiCollectionEnabled}) => setApiCollectionEnabled
    );

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const updateTagsMutation: UseMutationResult<void, object, any, unknown> = {} as any;

    const queryClient = useQueryClient();

    const deleteApiCollection = useDeleteApiCollectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const enableProjectInstanceMutation = useEnableProjectInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const handleOnCheckedChange = (value: boolean) => {
        enableProjectInstanceMutation.mutate(
            {
                enable: value,
                id: apiCollection.projectInstanceId!,
            },
            {
                onSuccess: () => {
                    setApiCollectionEnabled(apiCollection.projectInstanceId!, !apiCollection.enabled);
                    apiCollection!.enabled = !apiCollection.enabled;
                },
            }
        );
    };

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center border-b border-muted py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center justify-between">
                                {apiCollection.description ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="mr-2 text-base font-semibold">{apiCollection.name}</span>

                                            <Badge className="flex space-x-1" variant="secondary">
                                                <span>V{apiCollection.collectionVersion}</span>
                                            </Badge>
                                        </TooltipTrigger>

                                        <TooltipContent>{apiCollection.description}</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <div className="flex">
                                        <span className="mr-2 text-base font-semibold">{apiCollection.name}</span>

                                        <Badge className="flex space-x-1" variant="secondary">
                                            <span>V{apiCollection.collectionVersion}</span>
                                        </Badge>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center">
                                <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-gray-700">
                                    <span className="mr-1">
                                        {apiCollection.endpoints?.length === 1
                                            ? `1 endpoint`
                                            : `${apiCollection.endpoints?.length} endpoints`}
                                    </span>

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.preventDefault()}>
                                    {apiCollection.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequestModel: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={apiCollection.id!}
                                            remainingTags={[]}
                                            tags={apiCollection.tags}
                                            updateTagsMutation={updateTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex flex-col items-end gap-y-4">
                            <Switch checked={apiCollection.enabled} onCheckedChange={handleOnCheckedChange} />

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {apiCollection.lastModifiedDate ? (
                                        <>
                                            <CalendarIcon
                                                aria-hidden="true"
                                                className="mr-0.5 size-3.5 shrink-0 text-gray-400"
                                            />

                                            <span className="text-xs">
                                                {`Updated at ${apiCollection.lastModifiedDate?.toLocaleDateString()} ${apiCollection.lastModifiedDate?.toLocaleTimeString()}`}
                                            </span>
                                        </>
                                    ) : (
                                        '-'
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Updated Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost">
                                    <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                                </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                                <DropdownMenuItem onClick={() => setShowApiEndpointDialog(true)}>
                                    New Endpoint
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            {showDeleteDialog && (
                <DeleteAlertDialog
                    onCancel={() => setShowDeleteDialog(false)}
                    onDelete={() => deleteApiCollection.mutate(apiCollection.id!)}
                    open={showDeleteDialog}
                />
            )}

            {showEditDialog && (
                <ApiCollectionDialog apiCollection={apiCollection} onClose={() => setShowEditDialog(false)} />
            )}

            {showApiEndpointDialog && !!apiCollection.id && (
                <ApiCollectionEndpointDialog
                    apiCollectionId={apiCollection.id!}
                    onClose={() => setShowApiEndpointDialog(false)}
                    projectId={apiCollection.projectId}
                    projectVersion={apiCollection.projectVersion}
                />
            )}
        </>
    );
};

export default ApiCollectionListItem;
