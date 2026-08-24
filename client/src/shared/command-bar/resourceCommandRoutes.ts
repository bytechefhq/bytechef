import {type CommandActionType} from '@/shared/command-bar/types';
import {SearchAssetType} from '@/shared/middleware/graphql';

export interface SearchResultRouteInputI {
    id: string;
    knowledgeBaseId?: string;
    projectId?: string;
    projectWorkflowId?: string;
    type: SearchAssetType;
}

export const buildSearchResultRoute = (result: SearchResultRouteInputI): string | undefined => {
    switch (result.type) {
        case SearchAssetType.ApiCollection:
        case SearchAssetType.ApiEndpoint:
            return '/automation/api-platform/api-collections';
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

    return [{to, type: 'navigate'}];
}
