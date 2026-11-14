import {useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';

import {McpAutomationProjectConfigurationWorkflowItemType} from './useMcpAutomationProjectConfigurationList';

const useMcpAutomationProjectConfigurationWorkflowListItem = (
    workflow: McpAutomationProjectConfigurationWorkflowItemType
) => {
    const [showPropertiesPopover, setShowPropertiesPopover] = useState(false);

    const queryClient = useQueryClient();

    const invalidateWorkflows = () => {
        void queryClient.invalidateQueries({queryKey: ['mcpAutomationProjectConfigurationsByServerId']});
    };

    const enabledMutation = useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation({
        onSuccess: invalidateWorkflows,
    });

    const handleEnabledChange = (enabled: boolean) => enabledMutation.mutate({enabled, id: workflow.id});

    return {
        handleEnabledChange,
        isEnablePending: enabledMutation.isPending,
        setShowPropertiesPopover,
        showPropertiesPopover,
    };
};

export default useMcpAutomationProjectConfigurationWorkflowListItem;
