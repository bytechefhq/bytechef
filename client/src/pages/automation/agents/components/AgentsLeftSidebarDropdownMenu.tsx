import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useDeleteAiAgentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {MoreVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface AgentsLeftSidebarDropdownMenuPropsI {
    agent: {description?: string | null; id: string; title: string};
    /** True when this row is the agent currently open, so deleting it has to navigate away. */
    current: boolean;
}

/**
 * Per-row Edit/Delete menu for the agents sidebar, mirroring the data tables sidebar: hidden until the row is
 * hovered (or the menu is open), so a list of agents stays a list of names.
 */
const AgentsLeftSidebarDropdownMenu = ({agent, current}: AgentsLeftSidebarDropdownMenuPropsI) => {
    const [showEditDialog, setShowEditDialog] = useState(false);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const deleteAgentMutation = useDeleteAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to delete the agent.');
        },
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            // Only when the deleted agent is the one being viewed — deleting another row from the sidebar
            // should leave the user where they are.
            if (current) {
                navigate('/automation/agents');
            }
        },
    });

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label={`${agent.title} menu`}
                        className="w-6 opacity-0 transition-opacity group-hover:opacity-100 data-[state=open]:opacity-100"
                        icon={<MoreVerticalIcon className="h-4" />}
                        size="iconSm"
                        variant="ghost"
                    />
                </DropdownMenuTrigger>

                <DropdownMenuContent align="end">
                    <DropdownMenuItem onSelect={() => setShowEditDialog(true)}>
                        <PencilIcon className="mr-2 size-4" /> Edit
                    </DropdownMenuItem>

                    <DropdownMenuSeparator />

                    {/* variant rather than a colour class: the item's own muted-svg rule wins over one, leaving
                        the icon grey. */}

                    <DropdownMenuItem
                        disabled={deleteAgentMutation.isPending}
                        onSelect={() => deleteAgentMutation.mutate({id: agent.id})}
                        variant="destructive"
                    >
                        <Trash2Icon className="mr-2 size-4" /> Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            {/* Controlled: the menu item that opens it unmounts on select, so the dialog cannot hang off a
                trigger inside the menu. */}

            <AgentDialog agent={agent} onOpenChange={setShowEditDialog} open={showEditDialog} />
        </>
    );
};

export default AgentsLeftSidebarDropdownMenu;
