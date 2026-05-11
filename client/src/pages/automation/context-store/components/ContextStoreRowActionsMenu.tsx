import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ContextStore, useDeleteContextStoreMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {MoreVerticalIcon, PencilIcon, TrashIcon} from 'lucide-react';
import {useState} from 'react';

import ContextStoreFormDialog from './ContextStoreFormDialog';

type ContextStoreRowActionsMenuPropsType = {
    contextStore: ContextStore;
    isAdmin: boolean;
};

const ContextStoreRowActionsMenu = ({contextStore, isAdmin}: ContextStoreRowActionsMenuPropsType) => {
    const [renameOpen, setRenameOpen] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const queryClient = useQueryClient();

    const deleteMutation = useDeleteContextStoreMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['contextStores']});
            // Sources cascaded-delete server-side; refresh the sources list so it doesn't dangle stale rows.
            queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
        },
    });

    if (!isAdmin) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <button
                        aria-label={`Actions for ${contextStore.name}`}
                        className="rounded-md p-1 hover:bg-muted"
                        data-testid={`context-store-actions-${contextStore.id}`}
                    >
                        <MoreVerticalIcon className="size-4" />
                    </button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => setRenameOpen(true)}>
                        <PencilIcon className="mr-2 size-4" />
                        Rename
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem
                        className="text-red-600"
                        onClick={() => {
                            if (
                                window.confirm(
                                    `Delete Context Store "${contextStore.name}"? Every source, entity, and record ` +
                                        `under it will be deleted via cascade. This cannot be undone.`
                                )
                            ) {
                                deleteMutation.mutate({
                                    id: contextStore.id,
                                    workspaceId: String(currentWorkspaceId),
                                });
                            }
                        }}
                    >
                        <TrashIcon className="mr-2 size-4" />
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <ContextStoreFormDialog contextStore={contextStore} onOpenChange={setRenameOpen} open={renameOpen} />
        </>
    );
};

export default ContextStoreRowActionsMenu;
