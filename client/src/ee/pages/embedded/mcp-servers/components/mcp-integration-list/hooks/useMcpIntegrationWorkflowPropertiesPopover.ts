import {
    McpIntegrationWorkflow,
    useMcpIntegrationWorkflowPropertiesQuery,
    useUpdateMcpIntegrationWorkflowMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

export default function useMcpIntegrationWorkflowPropertiesPopover(
    mcpIntegrationWorkflow: McpIntegrationWorkflow,
    onClose: () => void
) {
    const queryClient = useQueryClient();

    const {data: propertiesData, isLoading} = useMcpIntegrationWorkflowPropertiesQuery({
        mcpIntegrationWorkflowId: mcpIntegrationWorkflow.id,
    });

    const updateMcpIntegrationWorkflowMutation = useUpdateMcpIntegrationWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpIntegrationsByServerId'],
            });

            onClose();
        },
    });

    const properties = useMemo(() => {
        if (!propertiesData?.mcpIntegrationWorkflowProperties) {
            return [];
        }

        return propertiesData.mcpIntegrationWorkflowProperties.filter(Boolean) as unknown as PropertyAllType[];
    }, [propertiesData]);

    const defaultValues = useMemo(() => {
        const propertyDefaults: Record<string, unknown> = {};

        for (const property of properties) {
            const propertyRecord = property as unknown as Record<string, unknown>;

            const resolvedDefault =
                propertyRecord.defaultValue ??
                propertyRecord.integerDefaultValue ??
                propertyRecord.numberDefaultValue ??
                propertyRecord.booleanDefaultValue ??
                propertyRecord.arrayDefaultValue ??
                propertyRecord.objectDefaultValue;

            if (property.name && resolvedDefault !== undefined && resolvedDefault !== null) {
                propertyDefaults[property.name] = resolvedDefault;
            }
        }

        const savedParameters = (mcpIntegrationWorkflow.parameters as Record<string, unknown>) ?? {};

        return {...propertyDefaults, ...savedParameters};
    }, [mcpIntegrationWorkflow.parameters, properties]);

    const form = useForm({
        defaultValues,
    });

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form]);

    const {control, formState, handleSubmit} = form;

    const handleFormSubmit = (values: Record<string, unknown>) => {
        const sanitize = (record: Record<string, unknown>): Record<string, unknown> =>
            Object.fromEntries(
                Object.entries(record).map(([key, value]) => {
                    if (value === '') {
                        return [key, null];
                    }

                    if (value && typeof value === 'object' && !Array.isArray(value)) {
                        return [key, sanitize(value as Record<string, unknown>)];
                    }

                    return [key, value];
                })
            );

        updateMcpIntegrationWorkflowMutation.mutate({
            id: mcpIntegrationWorkflow.id,
            input: {
                parameters: sanitize(values),
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
