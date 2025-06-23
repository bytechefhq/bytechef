/* eslint-disable sort-keys */
import {
    Connection,
    ConnectionApi,
    ConnectionTagApi,
    GetConnectionsRequest,
    Tag,
} from '@/ee/shared/middleware/embedded/configuration';
import {RequestI} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['embedded_connectionTags'],
    connections: ['embedded_connections'],
    filteredConnections: (filters: RequestI) => [...ConnectionKeys.connections, filters],
};

export const getConnectedUserConnectionsQuery =
    (connectedUserId: number, connectionIds?: Array<number>) => (request: RequestI, enabled?: boolean) =>
        useQuery<Connection[], Error>({
            queryKey: ConnectionKeys.filteredConnections(request),
            queryFn: () =>
                new ConnectionApi().getConnectedUserConnections({
                    componentName: request.componentName!,
                    connectedUserId,
                    connectionIds,
                }),
            enabled: enabled === undefined ? true : enabled,
        });

export const useGetConnectionsQuery = (filters: GetConnectionsRequest, enabled?: boolean) =>
    useQuery<Connection[], Error>({
        queryKey: ConnectionKeys.filteredConnections(filters),
        queryFn: () => new ConnectionApi().getConnections(filters),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<Tag[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });
