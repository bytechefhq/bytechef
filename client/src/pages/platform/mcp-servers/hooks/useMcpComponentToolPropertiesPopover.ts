import buildToolParametersPayload, {deepMerge} from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import {collectPropertyDefaults} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import pickChangedValues from '@/pages/platform/mcp-servers/utils/pickChangedValues';
import {toFormParameters} from '@/pages/platform/mcp-servers/utils/toolParameterConversion';
import {
    McpTool,
    useClusterElementDefinitionQuery,
    useUpdateEmbeddedMcpToolMutation,
    useUpdateMcpToolMutation,
} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

export default function useMcpComponentToolPropertiesPopover(
    componentName: string,
    componentVersion: number,
    mcpTool: McpTool,
    onClose: () => void,
    embedded = false
) {
    const queryClient = useQueryClient();

    const {data: clusterElementDefinition, isLoading} = useClusterElementDefinitionQuery({
        clusterElementName: mcpTool.name,
        componentName,
        componentVersion,
    });

    const handleUpdateSuccess = () => {
        void queryClient.invalidateQueries({
            queryKey: [embedded ? 'embeddedMcpComponentsByServerId' : 'mcpComponentsByServerId'],
        });

        onClose();
    };

    const updateEmbeddedMcpToolMutation = useUpdateEmbeddedMcpToolMutation({
        onSuccess: handleUpdateSuccess,
    });

    const updateMcpToolMutation = useUpdateMcpToolMutation({
        onSuccess: handleUpdateSuccess,
    });

    const properties = useMemo(() => {
        if (!clusterElementDefinition?.clusterElementDefinition?.properties) {
            return [];
        }

        return clusterElementDefinition.clusterElementDefinition.properties as unknown as PropertyAllType[];
    }, [clusterElementDefinition]);

    const defaultValues = useMemo(() => {
        const propertyDefaults = collectPropertyDefaults(properties);

        const savedParameters = toFormParameters((mcpTool.parameters as Record<string, unknown>) ?? {});

        return deepMerge(propertyDefaults, savedParameters);
    }, [mcpTool.parameters, properties]);

    const form = useForm<Record<string, unknown>>({
        defaultValues,
    });

    useEffect(() => {
        form.reset(defaultValues);
    }, [defaultValues, form]);

    const {handleSubmit} = form;

    const handleFormSubmit = (values: Record<string, unknown>) => {
        const variables = {
            id: mcpTool.id,
            input: {
                mcpComponentId: mcpTool.mcpComponentId,
                name: mcpTool.name,
                parameters: buildToolParametersPayload({
                    aiValues: {},
                    dirtyValues: pickChangedValues(values, defaultValues, (path) => form.getFieldState(path).isDirty),
                    savedParameters: mcpTool.parameters as Record<string, unknown> | null | undefined,
                }),
                version: mcpTool.version,
            },
        };

        if (embedded) {
            updateEmbeddedMcpToolMutation.mutate(variables);
        } else {
            updateMcpToolMutation.mutate(variables);
        }
    };

    return {
        form,
        handleFormSubmit,
        handleSubmit,
        isLoading,
        properties,
    };
}
