import {useDeleteKnowledgeBaseMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface UseKnowledgeBaseDeleteAlertDialogProps {
    knowledgeBaseId: string;
    onClose: () => void;
}

export default function useKnowledgeBaseDeleteAlertDialog({
    knowledgeBaseId,
    onClose,
}: UseKnowledgeBaseDeleteAlertDialogProps) {
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const deleteKnowledgeBaseMutation = useDeleteKnowledgeBaseMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['knowledgeBases']});

            toast('Knowledge base deleted successfully.');

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
