import {Form} from '@/components/ui/form';
import McpWorkflowToolForm from '@/pages/platform/mcp-servers/components/McpWorkflowToolForm';
import {McpWorkflowToolFormsType} from '@/pages/platform/mcp-servers/hooks/useMcpWorkflowToolForms';
import {deepMerge, expandDottedPaths} from '@/pages/platform/mcp-servers/utils/buildToolParametersPayload';
import {collectPropertyDefaults} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import {toFormParameters} from '@/pages/platform/mcp-servers/utils/toolParameterConversion';
import {PropertyAllType} from '@/shared/types';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

interface McpWorkflowDialogToolFormProps {
    isLoading: boolean;
    properties: PropertyAllType[];
    toolForms: McpWorkflowToolFormsType;
    workflowId: string;
}

const McpWorkflowDialogToolForm = ({isLoading, properties, toolForms, workflowId}: McpWorkflowDialogToolFormProps) => {
    const {aiValuesByWorkflowId, getToolForm, isNewWorkflow, savedParametersByWorkflowId, setToolFormInitialValues} =
        toolForms;

    const toolForm = getToolForm(workflowId);

    const form = useForm<Record<string, unknown>>({formControl: toolForm.formControl.formControl});

    const isNew = isNewWorkflow(workflowId);
    const aiValues = aiValuesByWorkflowId[workflowId];
    const savedParameters = savedParametersByWorkflowId.get(workflowId);

    const initialValues = useMemo(() => {
        const propertyDefaults = collectPropertyDefaults(properties);

        if (isNew) {
            return deepMerge(propertyDefaults, expandDottedPaths(aiValues ?? {}));
        }

        return deepMerge(propertyDefaults, toFormParameters(savedParameters ?? {}));
    }, [aiValues, isNew, properties, savedParameters]);

    useEffect(() => {
        if (isLoading) {
            return;
        }

        if (toolForm.initialValues && JSON.stringify(toolForm.initialValues) === JSON.stringify(initialValues)) {
            return;
        }

        form.reset(initialValues, {keepDirtyValues: true});

        setToolFormInitialValues(workflowId, initialValues);
    }, [form, initialValues, isLoading, setToolFormInitialValues, toolForm, workflowId]);

    if (isLoading) {
        return <p className="text-sm text-muted-foreground">Loading properties...</p>;
    }

    if (properties.length === 0) {
        return <p className="text-sm text-muted-foreground">No configurable properties for this tool.</p>;
    }

    return (
        <Form {...form}>
            <McpWorkflowToolForm form={form} properties={properties} />
        </Form>
    );
};

export default McpWorkflowDialogToolForm;
