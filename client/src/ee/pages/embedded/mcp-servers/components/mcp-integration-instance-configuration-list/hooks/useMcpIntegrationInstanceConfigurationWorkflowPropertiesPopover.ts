import buildToolParametersPayload, {deepMerge} from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import {collectPropertyDefaults} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import pickChangedValues from '@/pages/platform/mcp-servers/utils/pickChangedValues';
import {toFormParameters, toStoredWorkflowParameters} from '@/pages/platform/mcp-servers/utils/toolParameterConversion';
import {
    useMcpIntegrationInstanceConfigurationWorkflowPropertiesQuery,
    useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

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

    const defaultValues = useMemo(() => {
        const propertyDefaults = collectPropertyDefaults(properties);

        const savedParameters =
            (mcpIntegrationInstanceConfigurationWorkflow.parameters as Record<string, unknown>) ?? {};

        return deepMerge(propertyDefaults, toFormParameters(savedParameters));
    }, [mcpIntegrationInstanceConfigurationWorkflow.parameters, properties]);

    const form = useForm<Record<string, unknown>>({
        defaultValues,
    });

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form]);

    const {handleSubmit} = form;

    const handleFormSubmit = (values: Record<string, unknown>) => {
        updateMcpIntegrationInstanceConfigurationWorkflowMutation.mutate({
            id: mcpIntegrationInstanceConfigurationWorkflow.id,
            input: {
                parameters: toStoredWorkflowParameters(
                    buildToolParametersPayload({
                        aiValues: {},
                        dirtyValues: pickChangedValues(
                            values,
                            defaultValues,
                            (path) => form.getFieldState(path).isDirty
                        ),
                        savedParameters: mcpIntegrationInstanceConfigurationWorkflow.parameters as
                            Record<string, unknown> | null | undefined,
                    })
                ),
            },
        });
    };

    return {
        form,
        handleFormSubmit,
        handleSubmit,
        isLoading,
        properties,
    };
}
