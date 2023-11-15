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
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {ConnectionModel, TagModel} from '@/middleware/helios/connection';
import {useUpdateConnectionTagsMutation} from '@/mutations/connectionTags.mutations';
import {useDeleteConnectionMutation} from '@/mutations/connections.mutations';
import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionQuery,
} from '@/queries/componentDefinitions.queries';
import {ConnectionKeys} from '@/queries/connections.queries';
import {Component1Icon, DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import TagList from '../../../components/TagList/TagList';
import ConnectionDialog from './components/ConnectionDialog';

interface ConnectionListItemProps {
    connection: ConnectionModel;
    remainingTags?: TagModel[];
}

const ConnectionListItem = ({
    connection,
    remainingTags,
}: ConnectionListItemProps) => {
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
        <>
            <div className="flex items-center">
                <div className="w-10/12 flex-1">
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center">
                            {componentDefinition?.icon && (
                                <InlineSVG
                                    className="mr-1 h-6 w-6 flex-none"
                                    src={componentDefinition.icon}
                                />
                            )}

                            {!componentDefinition?.icon && (
                                <Component1Icon className="mr-1 h-6 w-6 flex-none" />
                            )}

                            <span className="mr-2 text-base font-semibold">
                                {connection.name}
                            </span>
                        </div>

                        <div className="ml-2 flex shrink-0">
                            <Badge
                                className={twMerge(
                                    connection.active &&
                                        'bg-success text-success-foreground hover:bg-success'
                                )}
                                variant="secondary"
                            >
                                {connection.active ? 'Active' : 'Not Active'}
                            </Badge>
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div
                            className="flex h-[38px] items-center"
                            onClick={(event) => event.preventDefault()}
                        >
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
                                    updateTagsMutation={
                                        updateConnectionTagsMutation
                                    }
                                />
                            )}
                        </div>

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            {connection.createdDate && (
                                <>
                                    <CalendarIcon
                                        aria-hidden="true"
                                        className="mr-0.5 h-4 w-4 shrink-0 text-gray-400"
                                    />

                                    <span>
                                        {`Created at ${connection.createdDate?.toLocaleDateString()} ${connection.createdDate?.toLocaleTimeString()}`}
                                    </span>
                                </>
                            )}
                        </div>
                    </div>
                </div>

                <div className="flex w-2/12 justify-end">
                    <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                            <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                        </DropdownMenuTrigger>

                        <DropdownMenuContent align="end">
                            <DropdownMenuItem
                                className="cursor-pointer text-xs text-gray-700"
                                onClick={() => setShowEditDialog(true)}
                            >
                                Edit
                            </DropdownMenuItem>

                            <DropdownMenuSeparator />

                            <DropdownMenuItem
                                className="cursor-pointer text-xs text-red-600"
                                onClick={() => setShowDeleteDialog(true)}
                            >
                                Delete
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </div>
            </div>

            {showDeleteDialog && (
                <AlertDialog open={showDeleteDialog}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>
                                Are you absolutely sure?
                            </AlertDialogTitle>

                            <AlertDialogDescription>
                                This action cannot be undone. This will
                                permanently delete the connection.
                            </AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel
                                onClick={() => setShowDeleteDialog(false)}
                            >
                                Cancel
                            </AlertDialogCancel>

                            <AlertDialogAction
                                className="bg-red-600"
                                onClick={() => {
                                    if (connection.id) {
                                        deleteConnectionMutation.mutate(
                                            connection.id
                                        );

                                        setShowDeleteDialog(false);
                                    }
                                }}
                            >
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            )}

            {showEditDialog && (
                <ConnectionDialog
                    connection={connection}
                    onClose={() => setShowEditDialog(false)}
                    showTrigger={false}
                    visible
                />
            )}
        </>
    );
};

export default ConnectionListItem;
