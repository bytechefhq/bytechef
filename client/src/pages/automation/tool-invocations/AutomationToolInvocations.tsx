import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ToolInvocations} from '@/pages/automation/tool-invocations/ToolInvocations';
import AutomationToolInvocationsEmptyState from '@/pages/automation/tool-invocations/components/AutomationToolInvocationsEmptyState';
import {PlatformType, useA2aServersQuery, useWorkspaceMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const AutomationToolInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data: a2aServersData} = useA2aServersQuery(
        {workspaceId: currentWorkspaceId + ''},
        {enabled: currentWorkspaceId != null}
    );

    const {data} = useWorkspaceMcpServersQuery(
        {workspaceId: currentWorkspaceId + ''},
        {enabled: currentWorkspaceId != null}
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
        <ToolInvocations
            a2aServerOptions={a2aServerOptions}
            firstRunEmptyState={<AutomationToolInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={`${currentWorkspaceId}-${currentEnvironmentId}`}
            mcpServerOptions={mcpServerOptions}
            type={PlatformType.Automation}
            workspaceScope={workspaceScope}
        />
    );
};
