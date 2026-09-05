import {WorkflowNodeOutput} from '@/shared/middleware/platform/configuration';

export default function getDataPillPanelNodeOutputs(
    workflowNodeOutputs: Array<WorkflowNodeOutput>,
    currentNodeName: string | undefined
): Array<WorkflowNodeOutput> {
    return workflowNodeOutputs.filter((workflowNodeOutput) => {
        const {actionDefinition, taskDispatcherDefinition, triggerDefinition, workflowNodeName} = workflowNodeOutput;

        if (workflowNodeName === currentNodeName) {
            return false;
        }

        return (
            actionDefinition?.outputDefined ||
            triggerDefinition?.outputDefined ||
            taskDispatcherDefinition?.outputDefined ||
            taskDispatcherDefinition?.variablePropertiesDefined
        );
    });
}
