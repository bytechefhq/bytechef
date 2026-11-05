import {
    ProvidedConnectionsType,
    ProvidedInputsType,
    ProvidedValuesPayloadI,
} from '../automation-workflow-project-configuration-provided-values/providedValues-utils';

export interface ConfigurationDialogWorkflowI {
    enabled: boolean;
    label: string;
    workflowUuid: string;
}

export interface WorkflowDraftI {
    enabled?: boolean;
    providedConnections?: ProvidedConnectionsType;
    providedInputs?: ProvidedInputsType;
    providedValuesPayload?: ProvidedValuesPayloadI;
}

export interface WorkflowEnableChangeI {
    enable: boolean;
    workflowUuid: string;
}

export interface WorkflowProvidedValuesChangeI {
    providedValuesPayload: ProvidedValuesPayloadI;
    workflowUuid: string;
}

export interface WorkflowChangesI {
    enableChanges: WorkflowEnableChangeI[];
    providedValuesChanges: WorkflowProvidedValuesChangeI[];
}

export const getWorkflowChanges = (
    workflows: ConfigurationDialogWorkflowI[],
    workflowDrafts: Record<string, WorkflowDraftI>
): WorkflowChangesI => {
    const enableChanges: WorkflowEnableChangeI[] = [];
    const providedValuesChanges: WorkflowProvidedValuesChangeI[] = [];

    for (const workflow of workflows) {
        const workflowDraft = workflowDrafts[workflow.workflowUuid];

        if (!workflowDraft) {
            continue;
        }

        if (workflowDraft.enabled !== undefined && workflowDraft.enabled !== workflow.enabled) {
            enableChanges.push({enable: workflowDraft.enabled, workflowUuid: workflow.workflowUuid});
        }

        if (workflowDraft.providedValuesPayload) {
            providedValuesChanges.push({
                providedValuesPayload: workflowDraft.providedValuesPayload,
                workflowUuid: workflow.workflowUuid,
            });
        }
    }

    return {enableChanges, providedValuesChanges};
};
