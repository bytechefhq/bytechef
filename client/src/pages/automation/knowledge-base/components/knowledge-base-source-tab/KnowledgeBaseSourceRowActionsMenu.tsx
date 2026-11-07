import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
    KnowledgeBaseSource,
    useDeleteKnowledgeBaseSourceMutation,
    useRefreshKnowledgeBaseSourceMutation,
    useSetKnowledgeBaseSourceEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {MoreHorizontalIcon, PowerIcon, RefreshCwIcon, TrashIcon} from 'lucide-react';

interface KnowledgeBaseSourceRowActionsMenuPropsI {
    isAdmin: boolean;
    source: KnowledgeBaseSource;
}

const KnowledgeBaseSourceRowActionsMenu = ({isAdmin, source}: KnowledgeBaseSourceRowActionsMenuPropsI) => {
    const queryClient = useQueryClient();

    const invalidateSources = () => {
        queryClient.invalidateQueries({queryKey: ['knowledgeBaseSources']});
    };

    const refreshMutation = useRefreshKnowledgeBaseSourceMutation();
    const deleteMutation = useDeleteKnowledgeBaseSourceMutation({onSuccess: invalidateSources});
    const setEnabledMutation = useSetKnowledgeBaseSourceEnabledMutation({onSuccess: invalidateSources});

    if (!isAdmin) {
        return null;
    }

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button
                    aria-label={`Actions for ${source.name}`}
                    className="rounded-md p-1 hover:bg-muted"
                    data-testid={`kbs-source-actions-${source.id}`}
                >
                    <MoreHorizontalIcon className="size-4" />
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => refreshMutation.mutate({id: source.id})}>
                    <RefreshCwIcon className="mr-2 size-4" />
                    Refresh now
                </DropdownMenuItem>

                <DropdownMenuItem onClick={() => setEnabledMutation.mutate({enabled: !source.enabled, id: source.id})}>
                    <PowerIcon className="mr-2 size-4" />

                    {source.enabled ? 'Disable' : 'Enable'}
                </DropdownMenuItem>

                <DropdownMenuSeparator />

                <DropdownMenuItem
                    onClick={() => {
                        if (window.confirm(`Delete source "${source.name}"? This cannot be undone.`)) {
                            deleteMutation.mutate({id: source.id});
                        }
                    }}
                    variant="destructive"
                >
                    <TrashIcon className="mr-2 size-4" />
                    Delete
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default KnowledgeBaseSourceRowActionsMenu;
