import {
    useDeleteMcpAutomationProjectConfigurationWorkflowMutation,
    useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {McpAutomationProjectConfigurationWorkflowItemType} from './useMcpAutomationProjectConfigurationList';

const useMcpAutomationProjectConfigurationWorkflowListItem = (
    workflow: McpAutomationProjectConfigurationWorkflowItemType
) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showPropertiesPopover, setShowPropertiesPopover] = useState(false);

    const queryClient = useQueryClient();

    const invalidateWorkflows = () => {
        void queryClient.invalidateQueries({queryKey: ['mcpAutomationProjectConfigurationsByServerId']});
    };

    const enabledMutation = useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation({
        onSuccess: invalidateWorkflows,
    });

    const deleteMutation = useDeleteMcpAutomationProjectConfigurationWorkflowMutation({
        onSuccess: () => {
            invalidateWorkflows();

            void queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});

            setShowDeleteDialog(false);
        },
    });

    const handleConfirmDelete = () => deleteMutation.mutate({id: workflow.id});

    const handleEnabledChange = (enabled: boolean) => enabledMutation.mutate({enabled, id: workflow.id});

    return {
        handleConfirmDelete,
        handleEnabledChange,
        isDeletePending: deleteMutation.isPending,
        isEnablePending: enabledMutation.isPending,
        setShowDeleteDialog,
        setShowPropertiesPopover,
        showDeleteDialog,
        showPropertiesPopover,
    };
};

export default useMcpAutomationProjectConfigurationWorkflowListItem;
