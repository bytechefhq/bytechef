import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {toast} from 'sonner';

import {AiAutoMemoryI, useDeleteAiAutoMemoryMutation} from '../hooks/useAiAutoMemories';

interface MemoryDeleteDialogProps {
    memory: AiAutoMemoryI | null;
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

const MemoryDeleteDialog = ({memory, onClose, open, workspaceId}: MemoryDeleteDialogProps) => {
    const deleteMemoryMutation = useDeleteAiAutoMemoryMutation();

    const handleDelete = async () => {
        if (!memory) {
            return;
        }

        try {
            await deleteMemoryMutation.mutateAsync({id: String(memory.id), workspaceId: String(workspaceId)});

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
