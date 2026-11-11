import {Form} from '@/components/ui/form';
import McpComponentToolForm from '@/pages/platform/mcp-servers/components/McpComponentToolForm';
import {deepMerge, expandDottedPaths} from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import {collectPropertyDefaults} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import {toFormParameters} from '@/pages/platform/mcp-servers/utils/toolParameterConversion';
import {useClusterElementDefinitionQuery} from '@/shared/middleware/graphql';
import {PropertyAllType} from '@/shared/types';
import {useEffect, useMemo} from 'react';
import {UseFormReturn, useForm} from 'react-hook-form';

export interface McpToolFormRegistrationI {
    form: UseFormReturn<Record<string, unknown>>;
    initialValues: Record<string, unknown>;
}

export type McpToolFormChangeHandlerType = (toolName: string, registration: McpToolFormRegistrationI | null) => void;

interface McpComponentDialogToolFormProps {
    aiValues?: Record<string, string>;
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    isNew: boolean;
    onFormChange: McpToolFormChangeHandlerType;
    savedParameters?: Record<string, unknown> | null;
    toolName: string;
}

const McpComponentDialogToolForm = ({
    aiValues,
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    isNew,
    onFormChange,
    savedParameters,
    toolName,
}: McpComponentDialogToolFormProps) => {
    const form = useForm<Record<string, unknown>>({defaultValues: {}});

    const {data: clusterElementDefinition, isLoading} = useClusterElementDefinitionQuery({
        clusterElementName: toolName,
        componentName,
        componentVersion,
    });

    const properties = useMemo(
        () => (clusterElementDefinition?.clusterElementDefinition?.properties ?? []) as unknown as PropertyAllType[],
        [clusterElementDefinition]
    );

    const formDefaultValues = useMemo(() => {
        const propertyDefaults = collectPropertyDefaults(properties);

        if (!isNew) {
            return deepMerge(propertyDefaults, toFormParameters(savedParameters ?? {}));
        }

        return deepMerge(propertyDefaults, expandDottedPaths(aiValues ?? {}));
    }, [aiValues, isNew, properties, savedParameters]);

    useEffect(() => {
        if (isLoading) {
            return;
        }

        form.reset(formDefaultValues, {keepDirtyValues: true});

        onFormChange(toolName, {form, initialValues: formDefaultValues});
    }, [form, formDefaultValues, isLoading, onFormChange, toolName]);

    useEffect(() => () => onFormChange(toolName, null), [onFormChange, toolName]);

    if (isLoading) {
        return <p className="text-sm text-muted-foreground">Loading properties...</p>;
    }

    if (properties.length === 0) {
        return <p className="text-sm text-muted-foreground">No configurable properties for this tool.</p>;
    }

    return (
        <Form {...form}>
            <McpComponentToolForm
                componentName={componentName}
                componentVersion={componentVersion}
                connectionId={connectionId}
                connectionRequired={connectionRequired}
                form={form}
                properties={properties}
                toolName={toolName}
            />
        </Form>
    );
};

export default McpComponentDialogToolForm;
