import {useDeleteMcpIntegrationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpIntegrationListItemDropdownMenu = (mcpIntegrationId: string) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteMcpIntegrationMutation = useDeleteMcpIntegrationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpIntegrationsByServerId'],
            });
            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpIntegrationMutation.mutate({
            id: mcpIntegrationId,
        });
    };

    return {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog};
};

export default useMcpIntegrationListItemDropdownMenu;
