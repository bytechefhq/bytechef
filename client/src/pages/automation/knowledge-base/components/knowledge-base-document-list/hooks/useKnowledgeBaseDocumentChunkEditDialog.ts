import {useToast} from '@/hooks/use-toast';
import {useKnowledgeBaseDocumentChunkEditDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkEditDialogStore';
import {useUpdateKnowledgeBaseDocumentChunkMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
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

    const {toast} = useToast();
    const queryClient = useQueryClient();

    const updateMutation = useUpdateKnowledgeBaseDocumentChunkMutation({
        onError: () => {
            toast({description: 'Failed to update chunk. Please try again.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBase', {id: knowledgeBaseId}]});

            toast({description: 'Chunk updated successfully.'});

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
