/* eslint-disable sort-keys */
import {ConnectionApi, ConnectionModel, ConnectionTagApi} from '@/middleware/automation/connection';
import {TagModel} from '@/middleware/platform/connection';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['projectConnectionTags'],
    connections: ['projectConnections'],
    filteredConnections: (filters: {
        id?: number;
        componentName?: string;
        connectionVersion?: number;
        tagId?: number;
    }) => [...ConnectionKeys.connections, filters],
};

export const useGetConnectionsQuery = (
    request: {
        componentName?: string;
        connectionVersion?: number;
        tagId?: number;
    },
    enabled?: boolean
) =>
    useQuery<ConnectionModel[], Error>({
        queryKey: ConnectionKeys.filteredConnections(request),
        queryFn: () => new ConnectionApi().getConnections(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetConnectionTagsQuery = () =>
    useQuery<TagModel[], Error>({
        queryKey: ConnectionKeys.connectionTags,
        queryFn: () => new ConnectionTagApi().getConnectionTags(),
    });

export const useGetWorkspaceConnectionsQuery = (
    request: {id: number; componentName?: string; connectionVersion?: number; tagId?: number},
    enabled?: boolean
) =>
    useQuery<ConnectionModel[], Error>({
        queryKey: ConnectionKeys.filteredConnections(request),
        queryFn: () => new ConnectionApi().getWorkspaceConnections(request),
        enabled: enabled === undefined ? true : enabled,
    });
