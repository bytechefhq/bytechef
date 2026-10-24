import invalidateDataSyncQueries from '@/pages/automation/data-syncs/utils/invalidateDataSyncQueries';
import {useDeleteDataSyncMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {toast} from 'sonner';

interface UseDataSyncActionsProps {
    dataSync: {id: string};
    onDeleted?: () => void;
}

const useDataSyncActions = ({dataSync, onDeleted}: UseDataSyncActionsProps) => {
    const [showDeleteConfirmDialog, setShowDeleteConfirmDialog] = useState(false);
    const [showEditDialog, setShowEditDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteDataSyncMutation = useDeleteDataSyncMutation({
        onError: (error) => {
            toast.error(error instanceof Error ? error.message : 'Failed to delete the data sync.');
        },
        onSuccess: () => {
            invalidateDataSyncQueries(queryClient, {projects: true});

            onDeleted?.();
        },
    });

    const handleDeleteClick = () => {
        setShowDeleteConfirmDialog(true);
    };

    const deleteDataSync = () => {
        setShowDeleteConfirmDialog(false);

        deleteDataSyncMutation.mutate({id: dataSync.id});
    };

    const openEditDialog = () => {
        setShowEditDialog(true);
    };

    return {
        deleteDataSync,
        handleDeleteClick,
        isDeleting: deleteDataSyncMutation.isPending,
        openEditDialog,
        setShowDeleteConfirmDialog,
        setShowEditDialog,
        showDeleteConfirmDialog,
        showEditDialog,
    };
};

export default useDataSyncActions;
