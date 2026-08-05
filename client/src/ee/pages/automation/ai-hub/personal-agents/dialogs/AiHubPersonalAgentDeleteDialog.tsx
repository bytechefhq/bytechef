import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {toast} from 'sonner';

import {AiHubPersonalAgentI, useDeleteAiHubPersonalAgentMutation} from '../hooks/useAiHubPersonalAgents';

interface AiHubPersonalAgentDeleteDialogPropsI {
    agent: AiHubPersonalAgentI | null;
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

/**
 * Confirmation dialog for personal-agent deletion. Server-side, the delete is non-cascading: existing chat
 * tasks bound to the agent stay accessible but new turns degrade to plain LLM behaviour. The copy
 * surfaces this so the user understands what survives a delete and what doesn't.
 */
const AiHubPersonalAgentDeleteDialog = ({agent, onOpenChange, open}: AiHubPersonalAgentDeleteDialogPropsI) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const deleteMutation = useDeleteAiHubPersonalAgentMutation();

    const handleDelete = async () => {
        if (!agent || currentWorkspaceId == null) {
            return;
        }

        try {
            await deleteMutation.mutateAsync({id: String(agent.id), workspaceId: String(currentWorkspaceId)});

            toast.success(`Personal agent "${agent.title ?? agent.name}" deleted`);

            onOpenChange(false);
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to delete personal agent');
        }
    };

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Delete this personal agent?</DialogTitle>

                    <DialogDescription>
                        {agent ? `"${agent.title ?? agent.name}" will be removed.` : 'This agent will be removed.'} Past
                        tasks remain visible after deletion, but new messages will run against the default agent rather
                        than this agent's instructions.
                    </DialogDescription>
                </DialogHeader>

                <DialogFooter>
                    <Button
                        disabled={deleteMutation.isPending}
                        label="Cancel"
                        onClick={() => onOpenChange(false)}
                        variant="outline"
                    />

                    <Button
                        disabled={!agent || deleteMutation.isPending}
                        label={deleteMutation.isPending ? 'Deleting...' : 'Delete'}
                        onClick={handleDelete}
                        variant="destructive"
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default AiHubPersonalAgentDeleteDialog;
