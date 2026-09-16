import Button from '@/components/Button/Button';
import {Dialog, DialogContent, DialogFooter, DialogHeader, DialogMain} from '@/components/Dialog';
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

    const handleDelete = () => {
        if (!memory) {
            return;
        }

        deleteMemoryMutation.mutate(
            {
                environment: environmentId,
                id: String(memory.id),
                principal: {
                    principalId: memory.principalId,
                    principalType: memory.principalType as AiAutoMemoryPrincipalType,
                },
                workspaceId: String(workspaceId),
            },
            {
                onSuccess: () => {
                    toast.success(`Memory "${memory.title}" deleted`);

                    onClose();
                },
            }
        );
    };

    return (
        <Dialog onOpenChange={(nextOpen) => !nextOpen && onClose()} open={open}>
            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description={
                            <>
                                {memory
                                    ? `"${memory.title}" will be removed from the agent's long-term memory.`
                                    : "This memory will be removed from the agent's long-term memory."}{' '}
                                The deletion is permanent and cannot be undone.
                            </>
                        }
                        title="Delete this memory permanently?"
                    />

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
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default MemoryDeleteDialog;
