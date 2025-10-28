import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Badge} from '@/components/ui/badge';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ApiCollectionDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionDialog';
import ApiCollectionEndpointDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog';
import ApiCollectionListItemDropDownMenu from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionListItemDropDownMenu';
import {useApiCollectionsEnabledStore} from '@/ee/pages/automation/api-platform/api-collections/stores/useApiCollectionsEnabledStore';
import {ApiCollection, Tag} from '@/ee/shared/middleware/automation/api-platform';
import {useUpdateApiCollectionTagsMutation} from '@/ee/shared/mutations/automation/apiCollectionTags.mutations';
import {ApiCollectionTagKeys} from '@/ee/shared/mutations/automation/apiCollectionTags.queries';
import {useDeleteApiCollectionMutation} from '@/ee/shared/mutations/automation/apiCollections.mutations';
import {ApiCollectionKeys} from '@/ee/shared/mutations/automation/apiCollections.queries';
import ProjectDeploymentDialog from '@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog';
import TagList from '@/shared/components/TagList';
import {useEnableProjectDeploymentMutation} from '@/shared/mutations/automation/projectDeployments.mutations';
import {useGetProjectDeploymentQuery} from '@/shared/queries/automation/projectDeployments.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon} from 'lucide-react';
import {useState} from 'react';

interface ApiCollectionListItemProps {
    apiCollection: ApiCollection;
    tags?: Tag[];
}

const ApiCollectionListItem = ({apiCollection, tags}: ApiCollectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showApiEndpointDialog, setShowApiEndpointDialog] = useState(false);
    const [showUpdateProjectVersionDialog, setShowUpdateProjectVersionDialog] = useState(false);

    const setApiCollectionEnabled = useApiCollectionsEnabledStore(
        ({setApiCollectionEnabled}) => setApiCollectionEnabled
    );

    const apiCollectionTagIds = apiCollection.tags?.map((tag) => tag.id);

    const {data: projectDeployment} = useGetProjectDeploymentQuery(apiCollection.projectDeploymentId!);

    const queryClient = useQueryClient();

    const updateApiCollectionTagsMutation = useUpdateApiCollectionTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
            queryClient.invalidateQueries({
                queryKey: ApiCollectionTagKeys.apiCollectionTags,
            });
        },
    });

    const deleteApiCollection = useDeleteApiCollectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const enableProjectDeploymentMutation = useEnableProjectDeploymentMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const handleOnCheckedChange = (value: boolean) => {
        enableProjectDeploymentMutation.mutate(
            {
                enable: value,
                id: apiCollection.projectDeploymentId!,
            },
            {
                onSuccess: () => {
                    setApiCollectionEnabled(apiCollection.projectDeploymentId!, !apiCollection.enabled);
                    apiCollection!.enabled = !apiCollection.enabled;
                },
            }
        );
    };

    const handleOnProjectDeploymentDialogClose = () => {
        queryClient
            .invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            })
            .then(() => setShowUpdateProjectVersionDialog(false));
    };

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 hover:bg-gray-50">
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="flex w-full items-center justify-between">
                                {apiCollection.description ? (
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <span className="mr-2 text-base font-semibold">{apiCollection.name}</span>
                                        </TooltipTrigger>

                                        <TooltipContent>{apiCollection.description}</TooltipContent>
                                    </Tooltip>
                                ) : (
                                    <span className="mr-2 text-base font-semibold">{apiCollection.name}</span>
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
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={apiCollection.id!}
                                            remainingTags={tags?.filter(
                                                (tag) => !apiCollectionTagIds?.includes(tag.id)
                                            )}
                                            tags={apiCollection.tags ?? []}
                                            updateTagsMutation={updateApiCollectionTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge className="flex space-x-1" variant="secondary">
                                    V{apiCollection.projectVersion}
                                </Badge>
                            </TooltipTrigger>

                            <TooltipContent>The project version</TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 flex-col items-end gap-y-4">
                            <Switch checked={apiCollection.enabled} onCheckedChange={handleOnCheckedChange} />

                            <Tooltip>
                                <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                    {apiCollection.lastModifiedDate ? (
                                        <span className="text-xs">
                                            {`Modified at ${apiCollection.lastModifiedDate?.toLocaleDateString()} ${apiCollection.lastModifiedDate?.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        '-'
                                    )}
                                </TooltipTrigger>

                                <TooltipContent>Last Updated Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <ApiCollectionListItemDropDownMenu
                            apiCollectionId={apiCollection.id!}
                            onDeleteClick={() => setShowDeleteDialog(true)}
                            onEditClick={() => setShowEditDialog(true)}
                            onNewEndpoint={() => setShowApiEndpointDialog(true)}
                            onUpdateProjectVersionClick={() => setShowUpdateProjectVersionDialog(true)}
                        />
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
                    collectionVersion={apiCollection.collectionVersion!}
                    contextPath={apiCollection.contextPath!}
                    onClose={() => setShowApiEndpointDialog(false)}
                    projectId={apiCollection.projectId}
                    projectVersion={apiCollection.projectVersion}
                />
            )}

            {showUpdateProjectVersionDialog && (
                <ProjectDeploymentDialog
                    onClose={handleOnProjectDeploymentDialogClose}
                    projectDeployment={projectDeployment}
                    updateProjectVersion={true}
                />
            )}
        </>
    );
};

export default ApiCollectionListItem;
