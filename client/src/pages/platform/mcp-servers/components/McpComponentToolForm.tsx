import McpToolFormFields from '@/pages/platform/mcp-servers/components/McpToolFormFields';
import useMcpToolOptionsLoader from '@/pages/platform/mcp-servers/hooks/useMcpToolOptionsLoader';
import {ClusterElementProvider} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import {ComponentOperationType} from '@/shared/middleware/graphql';
import useFormDisplayConditions from '@/shared/queries/platform/useFormDisplayConditions';
import {PropertyAllType} from '@/shared/types';
import {UseFormReturn} from 'react-hook-form';

interface McpComponentToolFormProps {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    form: UseFormReturn<Record<string, unknown>>;
    properties: PropertyAllType[];
    toolName: string;
}

const McpComponentToolForm = ({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    form,
    properties,
    toolName,
}: McpComponentToolFormProps) => {
    const formValues = form.watch();

    const {displayConditions, isEvaluating} = useFormDisplayConditions({
        componentName,
        componentVersion,
        operationName: toolName,
        operationType: ComponentOperationType.ClusterElement,
        parameters: formValues,
    });

    const loadOptions = useMcpToolOptionsLoader({
        componentName,
        componentVersion,
        connectionId,
        connectionRequired,
        toolName,
    });

    if (isEvaluating) {
        return <p className="text-sm text-muted-foreground">Loading properties...</p>;
    }

    return (
        <ClusterElementProvider
            value={{
                clusterElementName: toolName,
                componentName,
                componentVersion,
                connectionId: connectionId ? Number(connectionId) : undefined,
                connectionRequired,
                inputParameters: formValues,
            }}
        >
            <McpToolFormFields
                displayConditions={displayConditions}
                form={form}
                loadOptions={loadOptions}
                properties={properties}
                values={formValues}
            />
        </ClusterElementProvider>
    );
};

export default McpComponentToolForm;
