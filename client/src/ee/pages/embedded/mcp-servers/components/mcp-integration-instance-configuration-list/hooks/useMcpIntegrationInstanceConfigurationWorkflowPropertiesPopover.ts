import {
    useMcpIntegrationInstanceConfigurationWorkflowPropertiesQuery,
    useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

import {getMcpToolPropertyFormDefaults, sanitizeMcpToolParameters} from '../../hooks/mcpToolPropertyForm';
import {McpIntegrationInstanceConfigurationWorkflowItemType} from './useMcpIntegrationInstanceConfigurationList';

export default function useMcpIntegrationInstanceConfigurationWorkflowPropertiesPopover(
    mcpIntegrationInstanceConfigurationWorkflow: McpIntegrationInstanceConfigurationWorkflowItemType,
    onClose: () => void
) {
    const queryClient = useQueryClient();

    const {data: propertiesData, isLoading} = useMcpIntegrationInstanceConfigurationWorkflowPropertiesQuery({
        mcpIntegrationInstanceConfigurationWorkflowId: mcpIntegrationInstanceConfigurationWorkflow.id,
    });

    const updateMcpIntegrationInstanceConfigurationWorkflowMutation =
        useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation({
            onSuccess: () => {
                void queryClient.invalidateQueries({
                    queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
                });

                onClose();
            },
        });

    const properties = useMemo(() => {
        if (!propertiesData?.mcpIntegrationInstanceConfigurationWorkflowProperties) {
            return [];
        }

        return propertiesData.mcpIntegrationInstanceConfigurationWorkflowProperties.filter(
            Boolean
        ) as unknown as PropertyAllType[];
    }, [propertiesData]);

    const defaultValues = useMemo(
        () =>
            getMcpToolPropertyFormDefaults(
                properties,
                mcpIntegrationInstanceConfigurationWorkflow.parameters as Record<string, unknown> | null
            ),
        [mcpIntegrationInstanceConfigurationWorkflow.parameters, properties]
    );

    const form = useForm({
        defaultValues,
    });

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form]);

    const {control, formState, handleSubmit} = form;

    const handleFormSubmit = (values: Record<string, unknown>) => {
        updateMcpIntegrationInstanceConfigurationWorkflowMutation.mutate({
            id: mcpIntegrationInstanceConfigurationWorkflow.id,
            input: {
                parameters: sanitizeMcpToolParameters(values),
            },
        });
    };

    return {
        control,
        form,
        formState,
        handleFormSubmit,
        handleSubmit,
        isLoading,
        properties,
    };
}
