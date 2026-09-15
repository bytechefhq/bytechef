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
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQuery} from '@tanstack/react-query';

export const ConnectionKeys = {
    connection: (id: number) => [...ConnectionKeys.connections, id],
    connectionTags: ['automation_connectionTags'],
    connections: ['automation_connections'],
    filteredConnections: (filters: {
        id?: number;
        componentName?: string;
        connectionVersion?: number;
        tagId?: number;
    }) => [...ConnectionKeys.connections, filters],
    workspaceConnectionTags: (workspaceId: number, environmentId?: number) => [
        ...ConnectionKeys.connectionTags,
        workspaceId,
        environmentId,
    ],
};

export const useGetConnectionTagsQuery = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    return useQuery<Tag[], Error>({
        queryKey: ConnectionKeys.workspaceConnectionTags(currentWorkspaceId!, currentEnvironmentId),
        queryFn: () =>
            new ConnectionTagApi().getConnectionTags({environmentId: currentEnvironmentId, id: currentWorkspaceId!}),
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
