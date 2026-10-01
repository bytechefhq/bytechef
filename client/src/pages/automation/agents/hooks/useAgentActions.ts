import invalidateAgentQueries from '@/pages/automation/agents/utils/invalidateAgentQueries';
import {useDeleteAiAgentMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

interface UseAgentActionsProps {
    agentId: string;
    navigateOnDelete: boolean;
}

const useAgentActions = ({agentId, navigateOnDelete}: UseAgentActionsProps) => {
    const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const deleteAgentMutation = useDeleteAiAgentMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to delete the agent.');
        },
        onSuccess: () => {
            invalidateAgentQueries(queryClient);

            if (navigateOnDelete) {
                navigate('/automation/projects');
            }
        },
    });

    const handleDeleteClick = () => {
        setShowDeleteConfirmDialog(true);
    };

    const handleConfirmDelete = () => {
        setShowDeleteConfirmDialog(false);

        deleteAgentMutation.mutate({id: agentId});
    };

    return {
        deleteAgentMutation,
        handleConfirmDelete,
        handleDeleteClick,
        setShowDeleteConfirmDialog,
        setShowEditDialog,
        showDeleteConfirmDialog,
        showEditDialog,
    };
};

export default useAgentActions;
