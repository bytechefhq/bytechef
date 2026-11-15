import {
    useMcpAutomationProjectConfigurationWorkflowPropertiesQuery,
    useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

import {getMcpToolPropertyFormDefaults, sanitizeMcpToolParameters} from '../../hooks/mcpToolPropertyForm';
import {McpAutomationProjectConfigurationWorkflowItemType} from './useMcpAutomationProjectConfigurationList';

const useMcpAutomationProjectConfigurationWorkflowPropertiesPopover = (
    workflow: McpAutomationProjectConfigurationWorkflowItemType,
    onClose: () => void
) => {
    const queryClient = useQueryClient();

    const {data: propertiesData, isLoading} = useMcpAutomationProjectConfigurationWorkflowPropertiesQuery({
        mcpAutomationProjectConfigurationWorkflowId: workflow.id,
    });

    const updateParametersMutation = useUpdateMcpAutomationProjectConfigurationWorkflowParametersMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['mcpAutomationProjectConfigurationsByServerId']});

            onClose();
        },
    });

    const properties = useMemo(() => {
        if (!propertiesData?.mcpAutomationProjectConfigurationWorkflowProperties) {
            return [];
        }

        return propertiesData.mcpAutomationProjectConfigurationWorkflowProperties.filter(
            Boolean
        ) as unknown as PropertyAllType[];
    }, [propertiesData]);

    const defaultValues = useMemo(
        () => getMcpToolPropertyFormDefaults(properties, workflow.parameters as Record<string, unknown> | null),
        [properties, workflow.parameters]
    );

    const form = useForm<Record<string, unknown>>({defaultValues});

    const {control, formState, handleSubmit} = form;

    const handleFormSubmit = (values: Record<string, unknown>) => {
        updateParametersMutation.mutate({id: workflow.id, parameters: sanitizeMcpToolParameters(values)});
    };

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form]);

    return {
        control,
        form,
        formState,
        handleFormSubmit,
        handleSubmit,
        isLoading,
        isPending: updateParametersMutation.isPending,
        properties,
    };
};

export default useMcpAutomationProjectConfigurationWorkflowPropertiesPopover;
