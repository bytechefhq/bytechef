import {WorkflowInput} from '@/shared/middleware/platform/configuration';

interface WorkflowDefinitionInputI {
    componentName?: string;
    componentVersion?: number;
    groupName?: string;
}

/**
 * Inverse of {@link toWorkflowDefinitionInput}. Rebuilds the in-memory/REST shape (a nested
 * `componentReference`) from the flat workflow definition keys (`componentName`, `componentVersion`,
 * `groupName`) — the same conversion the server performs when parsing the definition. Used to keep the
 * local store consistent with what a reload would return after an optimistic save. Non-component inputs
 * pass through unchanged.
 */
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
