let workflowEditorReadOnly = false;

export function isWorkflowEditorReadOnly(): boolean {
    return workflowEditorReadOnly;
}

export function setWorkflowEditorReadOnly(readOnly: boolean): void {
    workflowEditorReadOnly = readOnly;
}
