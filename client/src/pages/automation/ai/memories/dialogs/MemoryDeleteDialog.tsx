import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {AiAutoMemoryPrincipalType} from '@/shared/middleware/graphql';
import {toast} from 'sonner';

import {AiAutoMemoryI, useDeleteAiAutoMemoryMutation} from '../hooks/useAiAutoMemories';

interface MemoryDeleteDialogProps {
    environmentId: number;
    memory: AiAutoMemoryI | null;
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

const MemoryDeleteDialog = ({environmentId, memory, onClose, open, workspaceId}: MemoryDeleteDialogProps) => {
    const deleteMemoryMutation = useDeleteAiAutoMemoryMutation();

    const handleDelete = async () => {
        if (!memory) {
            return;
        }

        try {
            await deleteMemoryMutation.mutateAsync({
                environment: environmentId,
                id: String(memory.id),
                // The row's own owner, always as a PAIR — the server rejects one without the other, and omitting
                // both would resolve to the signed-in user, which is not who owns a deployment-owned memory.
                principalId: memory.principalId,
                principalType: memory.principalType as AiAutoMemoryPrincipalType,
                workspaceId: String(workspaceId),
            });

            toast.success(`Memory "${memory.title}" deleted`);

            onClose();
        } catch (error) {
            toast.error(error instanceof Error ? error.message : 'Failed to delete memory');
        }
    };

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Delete this memory permanently?</DialogTitle>

                    <DialogDescription>
                        {memory
                            ? `"${memory.title}" will be removed from the agent's long-term memory.`
                            : "This memory will be removed from the agent's long-term memory."}{' '}
                        Deletions through this page bypass the 30-minute agent-undo window used on the Artifact History
                        page and cannot be reversed.
                    </DialogDescription>
                </DialogHeader>

                <DialogFooter>
                    <Button
                        disabled={deleteMemoryMutation.isPending}
                        label="Cancel"
                        onClick={onClose}
                        variant="outline"
                    />

                    <Button
                        disabled={!memory || deleteMemoryMutation.isPending}
                        label={deleteMemoryMutation.isPending ? 'Deleting...' : 'Delete permanently'}
                        onClick={handleDelete}
                        variant="destructive"
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default MemoryDeleteDialog;
