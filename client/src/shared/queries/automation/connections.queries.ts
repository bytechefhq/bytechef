/* eslint-disable sort-keys */
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    Connection,
    ConnectionApi,
    ConnectionTagApi,
    GetWorkspaceConnectionsRequest,
    Tag,
} from '@/shared/middleware/automation/configuration';
import {DEFINITION_STALE_TIME} from '@/shared/queries/queryConstants';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['automation_connectionTags'],
    workspaceConnectionTags: (workspaceId: number) => [...ConnectionKeys.connectionTags, workspaceId],
    connections: ['automation_connections'],
    filteredConnections: (filters: {
        id?: number;
        componentName?: string;
        connectionVersion?: number;
        tagId?: number;
    }) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionTagsQuery = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useQuery<Tag[], Error>({
        queryKey: ConnectionKeys.workspaceConnectionTags(currentWorkspaceId!),
        queryFn: () => new ConnectionTagApi().getConnectionTags({id: currentWorkspaceId!}),
        enabled: currentWorkspaceId !== undefined,
    });
};

export const useGetWorkspaceConnectionsQuery = (request: GetWorkspaceConnectionsRequest, enabled?: boolean) =>
    useQuery<Connection[], Error>({
        queryKey: ConnectionKeys.filteredConnections(request),
        queryFn: () => new ConnectionApi().getWorkspaceConnections(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: DEFINITION_STALE_TIME,
    });
