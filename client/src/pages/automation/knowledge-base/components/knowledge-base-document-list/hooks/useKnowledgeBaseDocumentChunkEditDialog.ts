import {useKnowledgeBaseDocumentChunkEditDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkEditDialogStore';
import {useUpdateKnowledgeBaseDocumentChunkMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

interface UseKnowledgeBaseDocumentChunkEditDialogProps {
    knowledgeBaseId: string;
}

export default function useKnowledgeBaseDocumentChunkEditDialog({
    knowledgeBaseId,
}: UseKnowledgeBaseDocumentChunkEditDialogProps) {
    const {chunk, clearDialog, content, setChunk, setContent} = useKnowledgeBaseDocumentChunkEditDialogStore(
        useShallow((state) => ({
            chunk: state.chunk,
            clearDialog: state.clearDialog,
            content: state.content,
            setChunk: state.setChunk,
            setContent: state.setContent,
        }))
    );
    const queryClient = useQueryClient();

    const updateMutation = useUpdateKnowledgeBaseDocumentChunkMutation({
        onError: () => {
            toast.error('Failed to update chunk. Please try again.');
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});

            toast('Chunk updated successfully.');

            clearDialog();
        },
    });

    const handleSave = () => {
        if (!chunk) {
            return;
        }

        updateMutation.mutate({
            id: chunk.id,
            knowledgeBaseDocumentChunk: {
                content,
            },
        });
    };

    return {
        content,
        handleClose: clearDialog,
        handleContentChange: setContent,
        handleOpen: setChunk,
        handleOpenChange: (open: boolean) => {
            if (!open) {
                clearDialog();
            }
        },
        handleSave,
        isPending: updateMutation.isPending,
        open: chunk !== null,
    };
}
