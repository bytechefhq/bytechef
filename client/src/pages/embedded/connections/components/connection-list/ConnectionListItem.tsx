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
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {Connection, Tag} from '@/shared/middleware/embedded/connection';
import {useUpdateConnectionTagsMutation} from '@/shared/mutations/embedded/connectionTags.mutations';
import {
    useDeleteConnectionMutation,
    useUpdateConnectionMutation,
} from '@/shared/mutations/embedded/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/shared/queries/embedded/connections.queries';
import {
    ComponentDefinitionKeys,
    useGetConnectionComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {EllipsisVerticalIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import TagList from '../../../../../components/TagList';

interface ConnectionListItemProps {
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = ({connection, remainingTags}: ConnectionListItemProps) => {
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

    return (
        <li key={connection.id}>
            <>
                <div className="group flex items-center rounded-md bg-white px-2 hover:bg-gray-50">
                    <div className="flex flex-1 items-center py-5">
                        <div className="flex-1">
                            <div className="flex items-center justify-between">
                                <div className="relative flex items-center gap-2">
                                    {componentDefinition?.icon && (
                                        <InlineSVG className="size-5 flex-none" src={componentDefinition.icon} />
                                    )}

                                    {!componentDefinition?.icon && <Component1Icon className="mr-1 size-5 flex-none" />}

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
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Badge variant="secondary">{connection.environment}</Badge>
                                </TooltipTrigger>

                                <TooltipContent>The environment</TooltipContent>
                            </Tooltip>

                            <div className="flex min-w-52 flex-col items-end gap-y-4">
                                {connection.credentialStatus === 'VALID' ? (
                                    <Badge className="uppercase" variant={connection.active ? 'success' : 'secondary'}>
                                        {connection.active ? 'Active' : 'Not Active'}
                                    </Badge>
                                ) : (
                                    <Badge className="uppercase" variant="destructive">
                                        {connection.credentialStatus}
                                    </Badge>
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
                                    <Button size="icon" variant="ghost">
                                        <EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem onClick={() => setShowEditDialog(true)}>Edit</DropdownMenuItem>

                                    <DropdownMenuSeparator />

                                    <DropdownMenuItem
                                        className="text-destructive"
                                        onClick={() => setShowDeleteDialog(true)}
                                    >
                                        Delete
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

                            <AlertDialogAction className="bg-destructive" onClick={handleAlertDeleteDialogClick}>
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>

                {showEditDialog && (
                    <ConnectionDialog
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
};

export default ConnectionListItem;
