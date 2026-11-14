import {useDeleteMcpAutomationProjectConfigurationMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

const useMcpAutomationProjectConfigurationListItemDropdownMenu = (mcpAutomationProjectConfigurationId: string) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const queryClient = useQueryClient();

    const deleteMcpAutomationProjectConfigurationMutation = useDeleteMcpAutomationProjectConfigurationMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['mcpAutomationProjectConfigurationsByServerId']});

            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => {
        deleteMcpAutomationProjectConfigurationMutation.mutate({id: mcpAutomationProjectConfigurationId});
    };

    return {
        handleConfirmDelete,
        isDeletePending: deleteMcpAutomationProjectConfigurationMutation.isPending,
        setShowDeleteDialog,
        showDeleteDialog,
    };
};

export default useMcpAutomationProjectConfigurationListItemDropdownMenu;
