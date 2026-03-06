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

        const rawParameters = mcpProjectWorkflow.parameters;
        const savedParameters =
            rawParameters && typeof rawParameters === 'object' && !Array.isArray(rawParameters)
                ? (rawParameters as Record<string, unknown>)
                : {};

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
        function sanitizeValue(value: unknown): unknown {
            if (value === '') {
                return null;
            }

            if (Array.isArray(value)) {
                return value.map((item) => sanitizeValue(item));
            }

            if (value && typeof value === 'object') {
                return sanitize(value as Record<string, unknown>);
            }

            return value;
        }

        function sanitize(record: Record<string, unknown>): Record<string, unknown> {
            return Object.fromEntries(
                Object.entries(record).map(([key, value]) => {
                    return [key, sanitizeValue(value)];
                })
            );
        }

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
