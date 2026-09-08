import {McpTool, useClusterElementDefinitionQuery, useUpdateMcpToolMutation} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

export default function useMcpComponentToolPropertiesPopover(
    componentName: string,
    componentVersion: number,
    mcpTool: McpTool,
    onClose: () => void
) {
    const queryClient = useQueryClient();

    const {data: clusterElementDefinition, isLoading} = useClusterElementDefinitionQuery({
        clusterElementName: mcpTool.name,
        componentName,
        componentVersion,
    });

    const updateMcpToolMutation = useUpdateMcpToolMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ['mcpComponentsByServerId'],
            });

            onClose();
        },
    });

    const properties = useMemo(() => {
        if (!clusterElementDefinition?.clusterElementDefinition?.properties) {
            return [];
        }

        return clusterElementDefinition.clusterElementDefinition.properties as unknown as PropertyAllType[];
    }, [clusterElementDefinition]);

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
                propertyRecord.objectDefaultValue ??
                propertyRecord.dateDefaultValue ??
                propertyRecord.dateTimeDefaultValue ??
                propertyRecord.timeDefaultValue;

            if (property.name && resolvedDefault !== undefined && resolvedDefault !== null) {
                propertyDefaults[property.name] = resolvedDefault;
            }
        }

        const savedParameters = (mcpTool.parameters as Record<string, unknown>) ?? {};

        return {...propertyDefaults, ...savedParameters};
    }, [mcpTool.parameters, properties]);

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

        updateMcpToolMutation.mutate({
            id: mcpTool.id,
            input: {
                mcpComponentId: mcpTool.mcpComponentId,
                name: mcpTool.name,
                parameters: sanitize(values),
                version: mcpTool.version,
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
