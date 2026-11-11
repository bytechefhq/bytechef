import buildToolParametersPayload, {deepMerge} from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import {collectPropertyDefaults} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import pickChangedValues from '@/pages/platform/mcp-servers/utils/pickChangedValues';
import {toFormParameters, toStoredWorkflowParameters} from '@/pages/platform/mcp-servers/utils/toolParameterConversion';
import {useMcpProjectWorkflowPropertiesQuery, useUpdateMcpProjectWorkflowMutation} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

import {McpProjectWorkflowItemType} from './useMcpProjectList';

export default function useMcpProjectWorkflowPropertiesPopover(
    mcpProjectWorkflow: McpProjectWorkflowItemType,
    onClose: () => void
) {
    const queryClient = useQueryClient();

    const {data: propertiesData, isLoading} = useMcpProjectWorkflowPropertiesQuery({
        mcpProjectWorkflowId: mcpProjectWorkflow.id,
    });

    const updateMcpProjectWorkflowMutation = useUpdateMcpProjectWorkflowMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ['mcpProjectsByServerId'],
            });

            onClose();
        },
    });

    const properties = useMemo(() => {
        if (!propertiesData?.mcpProjectWorkflowProperties) {
            return [];
        }

        return propertiesData.mcpProjectWorkflowProperties.filter(Boolean) as unknown as PropertyAllType[];
    }, [propertiesData]);

    const defaultValues = useMemo(() => {
        const propertyDefaults = collectPropertyDefaults(properties);

        const rawParameters = mcpProjectWorkflow.parameters;
        const savedParameters =
            rawParameters && typeof rawParameters === 'object' && !Array.isArray(rawParameters)
                ? (rawParameters as Record<string, unknown>)
                : {};

        return deepMerge(propertyDefaults, toFormParameters(savedParameters));
    }, [mcpProjectWorkflow.parameters, properties]);

    const form = useForm<Record<string, unknown>>({
        defaultValues,
    });

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form]);

    const {handleSubmit} = form;

    const handleFormSubmit = (values: Record<string, unknown>) => {
        updateMcpProjectWorkflowMutation.mutate({
            id: mcpProjectWorkflow.id,
            input: {
                parameters: toStoredWorkflowParameters(
                    buildToolParametersPayload({
                        aiValues: {},
                        dirtyValues: pickChangedValues(
                            values,
                            defaultValues,
                            (path) => form.getFieldState(path).isDirty
                        ),
                        savedParameters: mcpProjectWorkflow.parameters as Record<string, unknown> | null | undefined,
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
