import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import AgentDialog from '@/pages/automation/agents/components/AgentDialog';
import DeleteAgentAlertDialog from '@/pages/automation/agents/components/DeleteAgentAlertDialog';
import useAgentActions from '@/pages/automation/agents/hooks/useAgentActions';
import {MoreVerticalIcon, PencilIcon, Trash2Icon} from 'lucide-react';
import {twMerge} from 'tailwind-merge';

interface AgentsLeftSidebarDropdownMenuPropsI {
    agent: {description?: string | null; id: string; title: string};
    alwaysVisibleTrigger?: boolean;
    current: boolean;
}

const AgentsLeftSidebarDropdownMenu = ({agent, alwaysVisibleTrigger, current}: AgentsLeftSidebarDropdownMenuPropsI) => {
    const {
        deleteAgentMutation,
        handleConfirmDelete,
        handleDeleteClick,
        setShowDeleteConfirmDialog,
        setShowEditDialog,
        showDeleteConfirmDialog,
        showEditDialog,
    } = useAgentActions({
        agentId: agent.id,
        navigateOnDelete: current,
    });

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>
                    <Button
                        aria-label={`${agent.title} menu`}
                        className={twMerge(
                            'w-6 transition-opacity data-[state=open]:opacity-100',
                            !alwaysVisibleTrigger && 'opacity-0 group-hover:opacity-100'
                        )}
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

                    <DropdownMenuItem
                        disabled={deleteAgentMutation.isPending}
                        onSelect={handleDeleteClick}
                        variant="destructive"
                    >
                        <Trash2Icon className="mr-2 size-4" /> Delete
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <AgentDialog agent={agent} onOpenChange={setShowEditDialog} open={showEditDialog} />

            {showDeleteConfirmDialog && (
                <DeleteAgentAlertDialog
                    agentTitle={agent.title}
                    onClose={() => setShowDeleteConfirmDialog(false)}
                    onDelete={handleConfirmDelete}
                />
            )}
        </>
    );
};

export default AgentsLeftSidebarDropdownMenu;
