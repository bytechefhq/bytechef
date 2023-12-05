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

import TagList from '../../../../components/TagList';
import ConnectionDialog from './ConnectionDialog';

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

    const handleAlertDeleteDialogClick = () => {
        if (connection.id) {
            deleteConnectionMutation.mutate(connection.id);

            setShowDeleteDialog(false);
        }
    };

    return (
        <li key={connection.id}>
            <>
                <div className="group flex items-center rounded-md bg-white p-2 py-3 hover:bg-gray-50">
                    <div className="flex-1">
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
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex flex-col items-end gap-y-4">
                            <Badge
                                className={twMerge(
                                    connection.active &&
                                        'bg-success text-success-foreground hover:bg-success'
                                )}
                                variant="secondary"
                            >
                                {connection.active ? 'Active' : 'Not Active'}
                            </Badge>

                            {connection.createdDate && (
                                <Tooltip>
                                    <TooltipTrigger className="flex items-center text-sm text-gray-500 sm:mt-0">
                                        <CalendarIcon
                                            aria-hidden="true"
                                            className="mr-0.5 h-3.5 w-3.5 shrink-0 text-gray-400"
                                        />

                                        <span>
                                            {`Created at ${connection.createdDate?.toLocaleDateString()} ${connection.createdDate?.toLocaleTimeString()}`}
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>
                                        Created Date
                                    </TooltipContent>
                                </Tooltip>
                            )}
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button size="icon" variant="ghost">
                                    <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                                </Button>
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                    onClick={() => setShowEditDialog(true)}
                                >
                                    Edit
                                </DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    className="text-red-600"
                                    onClick={() => setShowDeleteDialog(true)}
                                >
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>

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
                                onClick={handleAlertDeleteDialogClick}
                            >
                                Delete
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>

                {showEditDialog && (
                    <ConnectionDialog
                        connection={connection}
                        onClose={() => setShowEditDialog(false)}
                    />
                )}
            </>
        </li>
    );
};

export default ConnectionListItem;
