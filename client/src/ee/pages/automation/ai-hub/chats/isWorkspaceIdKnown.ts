/**
 * Whether the workspace store holds a real workspace id. Right after login the store is still empty until the
 * workspace list loads, and a hub request sent in that window carries no usable workspace.
 */
export function isWorkspaceIdKnown(workspaceId: number | undefined): workspaceId is number {
    return workspaceId != null && workspaceId > 0;
}
