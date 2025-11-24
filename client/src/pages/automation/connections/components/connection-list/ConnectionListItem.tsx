import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LazyLoadSVG from '@/components/LazyLoadSVG/LazyLoadSVG';
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
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {Connection, Tag} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useUpdateConnectionTagsMutation} from '@/shared/mutations/automation/connectionTags.mutations';
import {
    useDeleteConnectionMutation,
    useUpdateConnectionMutation,
} from '@/shared/mutations/automation/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/automation/connections.queries';
import {
    ComponentDefinitionKeys,
    useGetConnectionComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {ComponentIcon, EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {memo, useState} from 'react';

import TagList from '../../../../../shared/components/TagList';

interface ConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = memo(({componentDefinitions, connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {data: componentDefinition} = useGetConnectionComponentDefinitionQuery({
        componentName: connection.componentName,
        connectionVersion: connection.connectionVersion,
    });

    const queryClient = useQueryClient();

    const deleteConnectionMutation = useDeleteConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });
        },
    });

    const updateConnectionTagsMutation = useUpdateConnectionTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });
        },
    });

    const handleAlertDeleteDialogClick = () => {
        if (connection.id) {
            deleteConnectionMutation.mutate(connection.id);

            setShowDeleteDialog(false);
        }
    };

    if (!componentDefinition) {
        return <></>;
    }

    return (
        <li key={connection.id}>
            <>
                <div className="group flex items-center rounded-md bg-white px-2 hover:bg-gray-50">
                    <div className="flex flex-1 items-center py-5">
                        <div className="flex-1">
                            <div className="flex items-center justify-between">
                                <div className="relative flex items-center gap-2">
                                    <LazyLoadSVG
                                        className="size-5 flex-none"
                                        preloader={<ComponentIcon />}
                                        src={componentDefinition.icon!}
                                    />

                                    <span className="text-base font-semibold">{connection.name}</span>
                                </div>
                            </div>

                            <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                                <div
                                    className="flex h-connection-list-item-taglist-height items-center"
                                    onClick={(event) => event.preventDefault()}
                                >
                                    {connection.tags && (
                                        <TagList
                                            getRequest={(id, tags) => ({
                                                id: id!,
                                                updateTagsRequest: {
                                                    tags: tags || [],
                                                },
                                            })}
                                            id={connection.id!}
                                            remainingTags={remainingTags}
                                            tags={connection.tags}
                                            updateTagsMutation={updateConnectionTagsMutation}
                                        />
                                    )}
                                </div>
                            </div>
                        </div>

                        <div className="flex items-center justify-end gap-x-6">
                            <div className="flex min-w-52 flex-col items-end gap-y-4">
                                {connection.credentialStatus === 'VALID' ? (
                                    <Badge
                                        className="uppercase"
                                        label={connection.active ? 'Active' : 'Not Active'}
                                        styleType={connection.active ? 'success-outline' : 'secondary-outline'}
                                        weight="semibold"
                                    />
                                ) : (
                                    <Badge
                                        className="uppercase"
                                        label={connection.credentialStatus ?? 'INVALID'}
                                        styleType="destructive-outline"
                                        weight="semibold"
                                    />
                                )}

                                {connection.createdDate && (
                                    <Tooltip>
                                        <TooltipTrigger className="flex items-center text-sm text-gray-500 sm:mt-0">
                                            <span className="text-xs">
                                                {`Created at ${connection.createdDate?.toLocaleDateString()} ${connection.createdDate?.toLocaleTimeString()}`}
                                            </span>
                                        </TooltipTrigger>

                                        <TooltipContent>Created Date</TooltipContent>
                                    </Tooltip>
                                )}
                            </div>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end" className="p-0">
                                    <DropdownMenuItem
                                        className="dropdown-menu-item"
                                        onClick={() => setShowEditDialog(true)}
                                    >
                                        <EditIcon /> Edit
                                    </DropdownMenuItem>

                                    <DropdownMenuSeparator className="m-0" />

                                    <DropdownMenuItem
                                        className="dropdown-menu-item-destructive"
                                        onClick={() => setShowDeleteDialog(true)}
                                    >
                                        <Trash2Icon /> Delete
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </div>
                    </div>
                </div>

                <AlertDialog open={showDeleteDialog}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Are you absolutely sure?</AlertDialogTitle>

                            <AlertDialogDescription>
                                This action cannot be undone. This will permanently delete the connection.
                            </AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                            <AlertDialogAction
                                className="bg-surface-destructive-primary shadow-none hover:bg-surface-destructive-primary-hover active:bg-surface-destructive-primary-active"
                                onClick={handleAlertDeleteDialogClick}
                            >
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>

                {showEditDialog && componentDefinitions && (
                    <ConnectionDialog
                        componentDefinitions={componentDefinitions}
                        connection={connection}
                        connectionTagsQueryKey={ConnectionKeys.connectionTags}
                        connectionsQueryKey={ConnectionKeys.connections}
                        onClose={() => setShowEditDialog(false)}
                        useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                        useUpdateConnectionMutation={useUpdateConnectionMutation}
                    />
                )}
            </>
        </li>
    );
});

ConnectionListItem.displayName = 'ConnectionListItem';

export default ConnectionListItem;
