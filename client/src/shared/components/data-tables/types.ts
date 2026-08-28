/**
 * Which data tables a list is showing.
 *
 * `WORKSPACE` is the automation surface: a workspace's tables, with its tags. `EMBEDDED` is the vendor console, where
 * an absent `ownerId` means every owner — the console-wide view — and a present one narrows to what that connected
 * user would see, shared tables included.
 *
 * Tags are workspace-scoped and have no embedded equivalent, so an embedded scope yields none rather than borrowing a
 * workspace's.
 */
export type DataTableScopeType = {type: 'WORKSPACE'; workspaceId: number} | {type: 'EMBEDDED'; ownerId?: number};
