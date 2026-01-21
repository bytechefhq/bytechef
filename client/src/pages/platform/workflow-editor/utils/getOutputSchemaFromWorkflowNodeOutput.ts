import {WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';

/**
 * Gets the output schema from a workflow node output, falling back to variableOutputResponse
 * when outputResponse is not available. This is important for loop task dispatchers which
 * provide variable properties (item, index) via variableOutputResponse.
 *
 * @param workflowNodeOutput - The workflow node output containing outputResponse and/or variableOutputResponse
 * @returns The output schema from outputResponse or variableOutputResponse, or undefined if neither exists
 */
export default function getOutputSchemaFromWorkflowNodeOutput(
    workflowNodeOutput: WorkflowNodeOutput | undefined
): PropertyAllType | undefined {
    return workflowNodeOutput?.outputResponse?.outputSchema || workflowNodeOutput?.variableOutputResponse?.outputSchema;
}
