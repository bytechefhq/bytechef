import {WorkflowInput} from '@/shared/middleware/platform/configuration';

interface WorkflowDefinitionInputI {
    componentName?: string;
    componentVersion?: number;
    groupName?: string;
}

export function fromWorkflowDefinitionInput(input: WorkflowInput): WorkflowInput {
    const {componentName, componentVersion, groupName, ...rest} = input as WorkflowInput & WorkflowDefinitionInputI;

    if (componentName == null || componentVersion == null || groupName == null) {
        return rest;
    }

    return {
        ...rest,
        componentReference: {componentName, componentVersion, groupName},
    };
}
