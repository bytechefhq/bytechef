import {ExternalInvocations} from '@/pages/automation/external-invocations/ExternalInvocations';
import AutomationExternalInvocationsEmptyState from '@/pages/automation/external-invocations/components/AutomationExternalInvocationsEmptyState';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {PlatformType, useA2aServersQuery, useWorkspaceMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const AutomationExternalInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    useLoadWorkspaceScopes(currentWorkspaceId);

    const canViewA2aServers = useHasWorkspaceScope(currentWorkspaceId, 'A2A_VIEW');
    const canViewMcpServers = useHasWorkspaceScope(currentWorkspaceId, 'MCP_VIEW');

    const {data: a2aServersData} = useA2aServersQuery(
        {workspaceId: currentWorkspaceId + ''},
        {enabled: currentWorkspaceId != null && canViewA2aServers}
    );

    const {data} = useWorkspaceMcpServersQuery(
        {workspaceId: currentWorkspaceId + ''},
        {enabled: currentWorkspaceId != null && canViewMcpServers}
    );

    const a2aServerOptions = useMemo(
        () =>
            (a2aServersData?.a2aServers ?? [])
                .filter((a2aServer) => a2aServer != null)
                .map((a2aServer) => ({label: a2aServer!.name, value: a2aServer!.id})),
        [a2aServersData]
    );

    const mcpServerOptions = useMemo(
        () =>
            (data?.workspaceMcpServers ?? [])
                .filter((mcpServer) => mcpServer != null)
                .map((mcpServer) => ({label: mcpServer!.name, value: mcpServer!.id})),
        [data]
    );

    const hasMcpServer = useMemo(
        () =>
            (data?.workspaceMcpServers ?? []).some(
                (mcpServer) => mcpServer != null && mcpServer.environmentId === String(currentEnvironmentId)
            ),
        [currentEnvironmentId, data]
    );

    const workspaceScope = useMemo(
        () => ({environmentId: currentEnvironmentId, workspaceId: currentWorkspaceId}),
        [currentEnvironmentId, currentWorkspaceId]
    );

    return (
        <ExternalInvocations
            a2aServerOptions={a2aServerOptions}
            firstRunEmptyState={<AutomationExternalInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={`${currentWorkspaceId}-${currentEnvironmentId}`}
            mcpServerOptions={mcpServerOptions}
            type={PlatformType.Automation}
            workspaceScope={workspaceScope}
        />
    );
};
