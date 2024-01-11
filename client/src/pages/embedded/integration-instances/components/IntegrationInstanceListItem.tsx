import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
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
import {IntegrationInstanceModel, IntegrationModel, TagModel} from '@/middleware/embedded/configuration';
import {useUpdateIntegrationInstanceTagsMutation} from '@/mutations/embedded/integrationInstanceTags.mutations';
import {
    useDeleteIntegrationInstanceMutation,
    useEnableIntegrationInstanceMutation,
} from '@/mutations/embedded/integrationInstances.mutations';
import {useIntegrationInstancesEnabledStore} from '@/pages/embedded/integration-instances/stores/useIntegrationInstancesEnabledStore';
import {IntegrationInstanceTagKeys} from '@/queries/embedded/integrationInstanceTags.queries';
import {IntegrationInstanceKeys} from '@/queries/embedded/integrationInstances.queries';
import {ChevronDownIcon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import TagList from '../../../../components/TagList';

interface IntegrationItemProps {
    integrationInstance: IntegrationInstanceModel;
    remainingTags?: TagModel[];
    integration: IntegrationModel;
}

const IntegrationInstanceListItem = ({integration, integrationInstance, remainingTags}: IntegrationItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const setIntegrationInstanceEnabled = useIntegrationInstancesEnabledStore(
        ({setIntegrationInstanceEnabled}) => setIntegrationInstanceEnabled
    );

    const queryClient = useQueryClient();

    const deleteIntegrationInstanceMutation = useDeleteIntegrationInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstances,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceTagKeys.integrationInstanceTags,
            });
        },
    });

    const updateIntegrationInstanceTagsMutation = useUpdateIntegrationInstanceTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstances,
            });
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceTagKeys.integrationInstanceTags,
            });
        },
    });

    const enableIntegrationInstanceMutation = useEnableIntegrationInstanceMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: IntegrationInstanceKeys.integrationInstances,
            });
        },
    });

    return (
        <>
            <div className="flex w-full items-center justify-between rounded-md px-2 py-5 hover:bg-gray-50">
                <div className="flex-1">
                    <div className="flex items-center justify-between">
                        <div className="flex w-full items-center justify-between gap-2">
                            {integrationInstance.description ? (
                                <Tooltip>
                                    <TooltipTrigger>
                                        <span className="text-base font-semibold">{integrationInstance.name}</span>
                                    </TooltipTrigger>

                                    <TooltipContent>{integrationInstance.description}</TooltipContent>
                                </Tooltip>
                            ) : (
                                <span className="text-base font-semibold">{integrationInstance.name}</span>
                            )}
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div className="flex items-center">
                            <CollapsibleTrigger className="group mr-4 flex text-xs font-semibold text-gray-700">
                                <span className="mr-1">
                                    {integration.workflowIds?.length === 1
                                        ? `1 workflow`
                                        : `${integration.workflowIds?.length} workflows`}
                                </span>

                                <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                            </CollapsibleTrigger>

                            <div onClick={(event) => event.preventDefault()}>
                                {integrationInstance.tags && (
                                    <TagList
                                        getRequest={(id, tags) => ({
                                            id: id!,
                                            updateTagsRequestModel: {
                                                tags: tags || [],
                                            },
                                        })}
                                        id={integrationInstance.id!}
                                        remainingTags={remainingTags}
                                        tags={integrationInstance.tags}
                                        updateTagsMutation={updateIntegrationInstanceTagsMutation}
                                    />
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="flex items-center justify-end gap-x-6">
                    <div className="flex flex-col items-end gap-y-4">
                        <Badge variant={integrationInstance.enabled ? 'success' : 'secondary'}>
                            {integrationInstance.enabled ? 'Enabled' : 'Disabled'}
                        </Badge>

                        <Tooltip>
                            <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                {integrationInstance.lastExecutionDate ? (
                                    <span>
                                        {`Executed at ${integrationInstance.lastExecutionDate?.toLocaleDateString()} ${integrationInstance.lastExecutionDate?.toLocaleTimeString()}`}
                                    </span>
                                ) : (
                                    '-'
                                )}
                            </TooltipTrigger>

                            <TooltipContent>Last Execution Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <Switch
                        checked={integrationInstance.enabled}
                        onCheckedChange={(value) => {
                            enableIntegrationInstanceMutation.mutate(
                                {
                                    enable: value,
                                    id: integrationInstance.id!,
                                },
                                {
                                    onSuccess: () => {
                                        setIntegrationInstanceEnabled(
                                            integrationInstance.id!,
                                            !integrationInstance.enabled
                                        );
                                        integrationInstance!.enabled = !integrationInstance.enabled;
                                    },
                                }
                            );
                        }}
                    />

                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <Button size="icon" variant="ghost">
                                <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                            </Button>
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

            <AlertDialog open={showDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                        <AlertDialogDescription>
                            This action cannot be undone. This will permanently delete the integration and workflows it
                            contains.
                        </AlertDialogDescription>
                    </AlertDialogHeader>

                    <AlertDialogFooter>
                        <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                        <AlertDialogAction
                            className="bg-red-600"
                            onClick={() => {
                                if (integrationInstance.id) {
                                    deleteIntegrationInstanceMutation.mutate(integrationInstance.id);
                                }
                            }}
                        >
                            Delete
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>

            {showEditDialog && (
                <></>
                // <IntegrationInstanceDialog
                //
                //     onClose={() => setShowEditDialog(false)}
                //
                //     integrationInstance={integrationInstance}
                //
                // />
            )}
        </>
    );
};

export default IntegrationInstanceListItem;
