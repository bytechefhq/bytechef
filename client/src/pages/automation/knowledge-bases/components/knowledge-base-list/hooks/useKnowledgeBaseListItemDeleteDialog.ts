import {useDeleteKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {toast} from 'sonner';

interface UseKnowledgeBaseListItemDeleteDialogProps {
    knowledgeBaseId: string;
    onClose: () => void;
}

export default function useKnowledgeBaseListItemDeleteDialog({
    knowledgeBaseId,
    onClose,
}: UseKnowledgeBaseListItemDeleteDialogProps) {
    const queryClient = useQueryClient();

    const deleteKnowledgeBaseMutation = useDeleteKnowledgeBaseMutation({
        onError: () => {
            toast.error('Failed to delete knowledge base.');
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast('Knowledge base deleted successfully.');
        },
    });

    const handleDeleteClick = () => {
        deleteKnowledgeBaseMutation.mutate({id: knowledgeBaseId});

        onClose();
    };

    const handleCancelClick = () => {
        onClose();
    };

    return {
        handleCancelClick,
        handleDeleteClick,
        isDeleting: deleteKnowledgeBaseMutation.isPending,
    };
}
