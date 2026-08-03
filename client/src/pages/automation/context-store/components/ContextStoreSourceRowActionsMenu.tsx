import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
    ContextStoreSource,
    useDeleteContextStoreSourceMutation,
    useRefreshContextStoreSourceMutation,
    useSetContextStoreSourceEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {MoreHorizontalIcon, PowerIcon, RefreshCwIcon, TrashIcon} from 'lucide-react';
import {useState} from 'react';
import {toast} from 'sonner';

interface ContextStoreSourceRowActionsMenuPropsI {
    isAdmin: boolean;
    source: ContextStoreSource;
}

const ContextStoreSourceRowActionsMenu = ({isAdmin, source}: ContextStoreSourceRowActionsMenuPropsI) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const invalidateSources = () => {
        queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
    };

    const refreshMutation = useRefreshContextStoreSourceMutation({
        onError: (mutationError: unknown) => {
            const message = mutationError instanceof Error ? mutationError.message : 'Failed to trigger sync';

            toast.error(message);
        },
        onSuccess: (data) => {
            toast.success(`Sync triggered for ${source.name} (job ${data.refreshContextStoreSource})`);

            invalidateSources();
            queryClient.invalidateQueries({queryKey: ['contextStoreSource']});
        },
    });
    const deleteMutation = useDeleteContextStoreSourceMutation({
        onSuccess: invalidateSources,
    });
    const setEnabledMutation = useSetContextStoreSourceEnabledMutation({
        onSuccess: invalidateSources,
    });

    if (!isAdmin) {
        return null;
    }

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <button
                        aria-label={`Actions for ${source.name}`}
                        className="rounded-md p-1 hover:bg-muted"
                        data-testid={`context-store-source-actions-${source.id}`}
                    >
                        <MoreHorizontalIcon className="size-4" />
                    </button>
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => refreshMutation.mutate({id: source.id})}>
                        <RefreshCwIcon className="mr-2 size-4" />
                        Refresh now
                    </DropdownMenuItem>

                    <DropdownMenuItem
                        onClick={() => setEnabledMutation.mutate({enabled: !source.enabled, id: source.id})}
                    >
                        <PowerIcon className="mr-2 size-4" />

                        {source.enabled ? 'Disable' : 'Enable'}
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    <DropdownMenuItem className="text-content-destructive" onClick={() => setShowDeleteDialog(true)}>
                        <TrashIcon className="mr-2 size-4" />
                        Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={() => {
                    setShowDeleteDialog(false);
                    deleteMutation.mutate({id: source.id});
                }}
                open={showDeleteDialog}
            />
        </>
    );
};

export default ContextStoreSourceRowActionsMenu;
