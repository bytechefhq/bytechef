import {useToast} from '@/hooks/use-toast';
import {useDeleteKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';

interface UseKnowledgeBaseDeleteAlertDialogProps {
    knowledgeBaseId: string;
    onClose: () => void;
}

export default function useKnowledgeBaseDeleteAlertDialog({
    knowledgeBaseId,
    onClose,
}: UseKnowledgeBaseDeleteAlertDialogProps) {
    const {toast} = useToast();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const deleteKnowledgeBaseMutation = useDeleteKnowledgeBaseMutation({
        onError: () => {
            toast({description: 'Failed to delete knowledge base.', variant: 'destructive'});
        },
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast({description: 'Knowledge base deleted successfully.'});

            navigate('/automation/knowledge-bases');
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
