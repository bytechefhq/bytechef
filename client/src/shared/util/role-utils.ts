/**
 * Renders a role or authority identifier as a human-readable label —
 * `ROLE_ADMIN` becomes `Admin`, `EDITOR` becomes `Editor`.
 *
 * This title-cases rather than looking the value up in a table, so a role added
 * server-side still renders readably without a client change. That matters because
 * the workspace role picker derives its options from the generated `WorkspaceRole`
 * enum for exactly that reason — a lookup table would leave a new role blank.
 */
export const getRoleLabel = (role: string): string =>
    role
        .replace(/^ROLE_/, '')
        .split('_')
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');

/**
 * The label a permission scope carries when it is already shown under its module's heading:
 * `WORKFLOW_VIEW` under Workflow reads as `View`.
 *
 * Falls back to the full label when the scope name does not start with its group's — the
 * grouping comes from the module's enum, not from the scope name, so nothing guarantees the
 * two share a prefix, and a blank label would leave an unlabelled checkbox.
 */
export const getScopeActionLabel = (groupName: string, scope: string): string =>
    scope.startsWith(`${groupName}_`) ? getRoleLabel(scope.slice(groupName.length + 1)) : getRoleLabel(scope);
