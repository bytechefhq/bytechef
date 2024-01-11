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
import {ConnectionModel, TagModel} from '@/middleware/automation/connection';
import {useUpdateConnectionTagsMutation} from '@/mutations/embedded/connectionTags.mutations';
import {
    useCreateConnectionMutation,
    useDeleteConnectionMutation,
    useUpdateConnectionMutation,
} from '@/mutations/embedded/connections.mutations';
import ConnectionDialog from '@/pages/platform/connection/components/ConnectionDialog';
import {ConnectionKeys} from '@/queries/automation/connections.queries';
import {useGetConnectionTagsQuery} from '@/queries/embedded/connections.queries';
import {ComponentDefinitionKeys, useGetComponentDefinitionQuery} from '@/queries/platform/componentDefinitions.queries';
import {Component1Icon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import TagList from '../../../../components/TagList';

interface ConnectionListItemProps {
    connection: ConnectionModel;
    remainingTags?: TagModel[];
}

const ConnectionListItem = ({connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: connection.componentName,
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

    return (
        <li key={connection.id}>
            <div className="group rounded-md bg-white p-2 py-3 hover:bg-gray-50">
                <div className="flex items-center">
                    <div className="flex-1">
                        <div className="flex items-center justify-between">
                            <div className="relative flex items-center gap-2">
                                {componentDefinition?.icon && (
                                    <InlineSVG className="size-6 flex-none" src={componentDefinition.icon} />
                                )}

                                {!componentDefinition?.icon && <Component1Icon className="mr-1 size-6 flex-none" />}

                                <span className="text-base font-semibold text-gray-900">{connection.name}</span>
                            </div>
                        </div>

                        <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex h-[38px] items-center" onClick={(event) => event.preventDefault()}>
                                {connection.tags && (
                                    <TagList
                                        getRequest={(id, tags) => ({
                                            id: id!,
                                            updateTagsRequestModel: {
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
                        <div className="flex flex-col items-end gap-y-4">
                            <Badge variant={connection.active ? 'success' : 'secondary'}>
                                {connection.active ? 'Active' : 'Not Active'}
                            </Badge>

                            {connection.createdDate && (
                                <Tooltip>
                                    <TooltipTrigger className="flex items-center text-sm text-gray-500">
                                        <span>
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
                                This action cannot be undone. This will permanently delete the connection.
                            </AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={() => setShowDeleteDialog(false)}>Cancel</AlertDialogCancel>

                            <AlertDialogAction
                                className="bg-red-600"
                                onClick={() => {
                                    if (connection.id) {
                                        deleteConnectionMutation.mutate(connection.id);

                                        setShowDeleteDialog(false);
                                    }
                                }}
                            >
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
                        triggerNode={<Button>Create Connection</Button>}
                        useCreateConnectionMutation={useCreateConnectionMutation}
                        useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                        useUpdateConnectionMutation={useUpdateConnectionMutation}
                    />
                )}
            </div>
        </li>
    );
};

export default ConnectionListItem;
