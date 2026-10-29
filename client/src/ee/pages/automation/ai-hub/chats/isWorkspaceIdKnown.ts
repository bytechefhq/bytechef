/** Whether the workspace store holds a real workspace id. */
export function isWorkspaceIdKnown(workspaceId: number | undefined): workspaceId is number {
    return workspaceId != null && workspaceId > 0;
}
