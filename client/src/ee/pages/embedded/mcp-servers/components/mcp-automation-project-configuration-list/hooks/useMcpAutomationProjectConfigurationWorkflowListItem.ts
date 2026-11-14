import {
    useUpdateMcpAutomationProjectConfigurationWorkflowEnabledMutation,
    useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation,
} from '@/shared/middleware/graphql';
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

    const parametersMutation = useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation({
        onSuccess: () => {
            invalidateWorkflows();

            setShowPropertiesPopover(false);
        },
    });

    const handleEnabledChange = (enabled: boolean) => enabledMutation.mutate({enabled, id: workflow.id});

    const handleParametersSave = (parameters: Record<string, unknown>) =>
        parametersMutation.mutate({id: workflow.id, parameters});

    return {
        handleEnabledChange,
        handleParametersSave,
        isEnablePending: enabledMutation.isPending,
        isParametersPending: parametersMutation.isPending,
        setShowPropertiesPopover,
        showPropertiesPopover,
    };
};

export default useMcpAutomationProjectConfigurationWorkflowListItem;
