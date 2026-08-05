import {
    useClusterElementDefinitionQuery,
    useSetAiHubUserConnectorToolParametersMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

interface ConnectorToolI {
    name: string;
    parameters?: unknown;
    title?: string | null;
}

/**
 * Backs the per-tool "Configure" popover on the Connectors page. Mirrors the MCP server's
 * useMcpComponentToolPropertiesPopover: loads the cluster element's input properties, seeds the form with the
 * tool's saved parameters (falling back to property defaults), and persists them via
 * setAiHubUserConnectorToolParameters. On save it invalidates the connectors query so the row reflects the
 * new parameters.
 */
export default function useAiHubConnectorToolPropertiesPopover(
    componentName: string,
    componentVersion: number,
    connectorId: string,
    tool: ConnectorToolI,
    workspaceId: string,
    onClose: () => void
) {
    const queryClient = useQueryClient();

    const {data: clusterElementDefinition, isLoading} = useClusterElementDefinitionQuery({
        clusterElementName: tool.name,
        componentName,
        componentVersion,
    });

    const setParametersMutation = useSetAiHubUserConnectorToolParametersMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({queryKey: ['aiHubUserConnectors']});

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

        const savedParameters = (tool.parameters as Record<string, unknown>) ?? {};

        return {...propertyDefaults, ...savedParameters};
    }, [tool.parameters, properties]);

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

        setParametersMutation.mutate({
            connectorId,
            parameters: sanitize(values),
            toolName: tool.name,
            workspaceId,
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
