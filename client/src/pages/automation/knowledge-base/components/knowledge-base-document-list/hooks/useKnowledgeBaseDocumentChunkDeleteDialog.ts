import {useToast} from '@/hooks/use-toast';
import {useKnowledgeBaseDocumentChunkDeleteDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkDeleteDialogStore';
import {useKnowledgeBaseDocumentChunkSelectionStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore';
import {useDeleteKnowledgeBaseDocumentChunkMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

interface UseKnowledgeBaseDocumentChunkDeleteDialogProps {
    knowledgeBaseId: string;
}

export default function useKnowledgeBaseDocumentChunkDeleteDialog({
    knowledgeBaseId,
}: UseKnowledgeBaseDocumentChunkDeleteDialogProps) {
    const [isDeleting, setIsDeleting] = useState(false);

    const {chunkIdsToDelete, clearDialog, setChunkIdsToDelete} = useKnowledgeBaseDocumentChunkDeleteDialogStore(
        useShallow((state) => ({
            chunkIdsToDelete: state.chunkIdsToDelete,
            clearDialog: state.clearDialog,
            setChunkIdsToDelete: state.setChunkIdsToDelete,
        }))
    );

    const {clearSelection, selectedChunks} = useKnowledgeBaseDocumentChunkSelectionStore(
        useShallow((state) => ({
            clearSelection: state.clearSelection,
            selectedChunks: state.selectedChunks,
        }))
    );

    const {toast} = useToast();
    const queryClient = useQueryClient();

    const deleteMutation = useDeleteKnowledgeBaseDocumentChunkMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});
            clearSelection();
        },
    });

    const handleClose = () => {
        clearDialog();
    };

    const handleConfirm = async () => {
        if (chunkIdsToDelete.length === 0) {
            return;
        }

        setIsDeleting(true);

        try {
            const deletePromises = chunkIdsToDelete.map(
                (chunkId) =>
                    new Promise<void>((resolve, reject) => {
                        deleteMutation.mutate(
                            {id: chunkId},
                            {
                                onError: (error) => {
                                    reject(error);
                                },
                                onSuccess: () => {
                                    resolve();
                                },
                            }
                        );
                    })
            );

            const results = await Promise.allSettled(deletePromises);

            const failedCount = results.filter((result) => result.status === 'rejected').length;
            const successCount = results.filter((result) => result.status === 'fulfilled').length;

            if (failedCount > 0) {
                toast({
                    description: `Failed to delete ${failedCount} chunk${failedCount > 1 ? 's' : ''}. Please try again.`,
                    variant: 'destructive',
                });
            }

            if (successCount > 0) {
                toast({
                    description: `${successCount} chunk${successCount > 1 ? 's' : ''} deleted successfully.`,
                });
            }
        } finally {
            setIsDeleting(false);
            clearDialog();
        }
    };

    const handleDeleteChunk = (chunkId: string) => {
        setChunkIdsToDelete([chunkId]);
    };

    const handleDeleteChunks = (chunkIds: string[]) => {
        if (chunkIds.length === 0) {
            return;
        }

        setChunkIdsToDelete([...chunkIds]);
    };

    const handleDeleteSelectedChunks = () => {
        handleDeleteChunks(selectedChunks);
    };

    const handleOpenChange = (open: boolean) => {
        if (!open) {
            handleClose();
        }
    };

    return {
        chunkCount: chunkIdsToDelete.length,
        handleClose,
        handleConfirm,
        handleDeleteChunk,
        handleDeleteSelectedChunks,
        handleOpenChange,
        isPending: isDeleting || deleteMutation.isPending,
        open: chunkIdsToDelete.length > 0,
    };
}
