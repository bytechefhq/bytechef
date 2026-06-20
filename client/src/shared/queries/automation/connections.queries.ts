/* eslint-disable sort-keys */
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
    connectionTags: (id: number) => ['automation_connectionTags', id] as const,
    connections: ['automation_connections'],
    filteredConnections: (filters: {
        id?: number;
        componentName?: string;
        connectionVersion?: number;
        tagId?: number;
    }) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionTagsQuery = (id: number) =>
    useQuery<Tag[], Error>({
        queryKey: ConnectionKeys.connectionTags(id),
        queryFn: () => new ConnectionTagApi().getConnectionTags({id}),
    });

export const useGetWorkspaceConnectionsQuery = (request: GetWorkspaceConnectionsRequest, enabled?: boolean) =>
    useQuery<Connection[], Error>({
        queryKey: ConnectionKeys.filteredConnections(request),
        queryFn: () => new ConnectionApi().getWorkspaceConnections(request),
        enabled: enabled === undefined ? true : enabled,
        staleTime: DEFINITION_STALE_TIME,
    });
