import {type CommandActionType} from '@/shared/command-bar/types';
import {SearchAssetType} from '@/shared/middleware/graphql';

export interface SearchResultRouteInputI {
    id: string;
    knowledgeBaseId?: string;
    projectId?: string;
    projectWorkflowId?: string;
    type: SearchAssetType;
}

/**
 * Maps a search result to a route that actually exists in routes.tsx. Five asset types (deployments, connections,
 * API collections, API endpoints, knowledge base documents) have no detail route at all -- they are rendered by list
 * pages with in-page selection -- so they resolve to their list page.
 */
export const buildSearchResultRoute = (result: SearchResultRouteInputI): string | undefined => {
    switch (result.type) {
        case SearchAssetType.ApiCollection:
        case SearchAssetType.ApiEndpoint:
            return '/automation/api-platform/api-collections';
        case SearchAssetType.AssetFile:
            return `/automation/asset-files/${result.id}`;
        case SearchAssetType.Connection:
            return '/automation/connections';
        case SearchAssetType.DataTable:
            return `/automation/datatables/${result.id}`;
        case SearchAssetType.Deployment:
            return '/automation/deployments';
        case SearchAssetType.KnowledgeBase:
            return `/automation/knowledge-bases/${result.id}`;
        case SearchAssetType.KnowledgeBaseDocument:
            return result.knowledgeBaseId ? `/automation/knowledge-bases/${result.knowledgeBaseId}` : undefined;
        case SearchAssetType.Project:
            return result.projectWorkflowId
                ? `/automation/projects/${result.id}/project-workflows/${result.projectWorkflowId}`
                : '/automation/projects';
        case SearchAssetType.Workflow:
            return result.projectId
                ? `/automation/projects/${result.projectId}/project-workflows/${result.id}`
                : undefined;
        default:
            return undefined;
    }
};

/**
 * Five asset types have no detail route -- they are rendered by list pages with in-page selection -- so their
 * eventual command would navigate to the list and publish a select intent carrying the id, for a page to pick up and
 * highlight/open the right row. Kept as the declared key namespace for that future wiring, even though no page
 * claims any of them yet (see the publication note below).
 */
export const SELECT_INTENT_KEYS: Partial<Record<SearchAssetType, string>> = {
    [SearchAssetType.ApiCollection]: 'apiCollection.select',
    [SearchAssetType.ApiEndpoint]: 'apiEndpoint.select',
    [SearchAssetType.Connection]: 'connection.select',
    [SearchAssetType.Deployment]: 'deployment.select',
    [SearchAssetType.KnowledgeBaseDocument]: 'knowledgeBaseDocument.select',
};

export function buildSearchResultActions(result: SearchResultRouteInputI): CommandActionType[] {
    const to = buildSearchResultRoute(result);

    if (!to) {
        return [];
    }

    // Publishing a select intent here is disabled until a page actually opts in to claiming one of the
    // SELECT_INTENT_KEYS above: with no claimant, every published intent expires unclaimed and fires the DEV
    // "unclaimed intent" warning on the happy path, training developers to ignore the one warning that matters. It
    // also risks evicting a pending create intent, since the store holds only one intent and `publish` overwrites.
    // Re-enabling for a given type is a one-line change once its page claims the key.
    return [{to, type: 'navigate'}];
}
