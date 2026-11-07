import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {WorkflowInputType} from '@/shared/types';

export function toWorkflowDefinitionInput(input: WorkflowInputType): WorkflowInput {
    const {componentReference, ...rest} = input;

    delete rest.testValue;

    if (!componentReference) {
        return rest as WorkflowInput;
    }

    return {
        ...rest,
        componentName: componentReference.componentName,
        componentVersion: componentReference.componentVersion,
        groupName: componentReference.groupName,
    } as WorkflowInput;
}
