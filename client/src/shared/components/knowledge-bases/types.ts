/**
 * Which knowledge bases a list is showing. The data table twin of this type carries the same reasoning; see
 * `@/shared/components/data-tables/types`.
 *
 * `WORKSPACE` is the automation surface, with its tags. `EMBEDDED` is the vendor console, where an absent `ownerId`
 * means every owner and a present one narrows to what that connected user would see, shared knowledge bases included.
 */
export type KnowledgeBaseScopeType = {type: 'WORKSPACE'; workspaceId: number} | {type: 'EMBEDDED'; ownerId?: number};
