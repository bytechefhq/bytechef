import {useDeleteMcpProjectMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpProjectListItemDropdownMenu = (mcpProjectId: string) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteMcpProjectMutation = useDeleteMcpProjectMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            });
            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpProjectMutation.mutate({
            id: mcpProjectId,
        });
    };

    return {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog};
};

export default useMcpProjectListItemDropdownMenu;
