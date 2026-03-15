import {useDeleteMcpIntegrationInstanceConfigurationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpIntegrationInstanceConfigurationListItemDropdownMenu = (mcpIntegrationInstanceConfigurationId: string) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteMcpIntegrationInstanceConfigurationMutation = useDeleteMcpIntegrationInstanceConfigurationMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
            });
            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpIntegrationInstanceConfigurationMutation.mutate({
            id: mcpIntegrationInstanceConfigurationId,
        });
    };

    return {handleConfirmDelete, setShowDeleteDialog, showDeleteDialog};
};

export default useMcpIntegrationInstanceConfigurationListItemDropdownMenu;
