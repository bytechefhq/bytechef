import {useToast} from '@/hooks/use-toast';
import {useDeleteKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';

interface UseKnowledgeBaseListItemDeleteDialogProps {
    knowledgeBaseId: string;
    onClose: () => void;
}

export default function useKnowledgeBaseListItemDeleteDialog({
    knowledgeBaseId,
    onClose,
}: UseKnowledgeBaseListItemDeleteDialogProps) {
    const {toast} = useToast();
    const queryClient = useQueryClient();

    const deleteKnowledgeBaseMutation = useDeleteKnowledgeBaseMutation({
        onError: () => {
            toast({description: 'Failed to delete knowledge base.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast({description: 'Knowledge base deleted successfully.'});
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
