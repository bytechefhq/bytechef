import {ComponentOperationType, McpTool} from '@/shared/middleware/graphql';
import useFormDisplayConditions from '@/shared/queries/platform/useFormDisplayConditions';

const useMcpToolFormDisplayConditions = (
    componentName: string,
    componentVersion: number,
    mcpTool: McpTool,
    formValues: Record<string, unknown>
) =>
    useFormDisplayConditions({
        componentName,
        componentVersion,
        operationName: mcpTool.name,
        operationType: ComponentOperationType.ClusterElement,
        parameters: formValues,
    });

export default useMcpToolFormDisplayConditions;
