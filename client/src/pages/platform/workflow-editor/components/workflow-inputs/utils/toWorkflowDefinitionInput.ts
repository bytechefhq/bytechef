import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {WorkflowInputType} from '@/shared/types';

/**
 * Converts a workflow input from its in-memory/REST shape (a nested `componentReference` object) into
 * the workflow definition shape, where a component reference is persisted as flat keys —
 * `componentName`, `componentVersion`, `groupName`. The server parses these flat keys back into a
 * `ComponentInputReference`, so the definition must not carry the nested object. Non-component inputs
 * are returned unchanged (minus the editor-only `testValue`).
 */
export function toWorkflowDefinitionInput(input: WorkflowInputType): WorkflowInput {
    const {componentReference, ...rest} = input;

    // The editor-only test value never belongs in the persisted definition.
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
