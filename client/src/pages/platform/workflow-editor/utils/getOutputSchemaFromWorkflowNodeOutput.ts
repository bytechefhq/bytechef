import {WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';
import {PropertyAllType} from '@/shared/types';

export default function getOutputSchemaFromWorkflowNodeOutput(
    workflowNodeOutput: WorkflowNodeOutput | undefined
): PropertyAllType | undefined {
    return workflowNodeOutput?.outputResponse?.outputSchema || workflowNodeOutput?.variableOutputResponse?.outputSchema;
}
