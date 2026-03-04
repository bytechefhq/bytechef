import {
    McpProjectWorkflow,
    useMcpProjectWorkflowPropertiesQuery,
    useUpdateMcpProjectWorkflowMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

export default function useMcpProjectWorkflowPropertiesPopover(
    mcpProjectWorkflow: McpProjectWorkflow,
    onClose: () => void
) {
    const queryClient = useQueryClient();

    const {data: propertiesData, isLoading} = useMcpProjectWorkflowPropertiesQuery({
        mcpProjectWorkflowId: mcpProjectWorkflow.id,
    });

    const updateMcpProjectWorkflowMutation = useUpdateMcpProjectWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
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

        const savedParameters = (mcpProjectWorkflow.parameters as Record<string, unknown>) ?? {};

        return {...propertyDefaults, ...savedParameters};
    }, [mcpProjectWorkflow.parameters, properties]);

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

        updateMcpProjectWorkflowMutation.mutate({
            id: mcpProjectWorkflow.id,
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
