import McpToolFormFields from '@/pages/platform/mcp-servers/components/McpToolFormFields';
import {PropertyAllType} from '@/shared/types';
import {UseFormReturn} from 'react-hook-form';

interface McpWorkflowToolFormProps {
    form: UseFormReturn<Record<string, unknown>>;
    properties: PropertyAllType[];
}

const McpWorkflowToolForm = ({form, properties}: McpWorkflowToolFormProps) => {
    const formValues = form.watch();

    return <McpToolFormFields form={form} properties={properties} values={formValues} />;
};

export default McpWorkflowToolForm;
