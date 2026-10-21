import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ToolInvocations} from '@/pages/automation/tool-invocations/ToolInvocations';
import AutomationToolInvocationsEmptyState from '@/pages/automation/tool-invocations/components/AutomationToolInvocationsEmptyState';
import {PlatformType, useAppsQuery, useWorkspaceMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const AutomationToolInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data} = useWorkspaceMcpServersQuery(
        {workspaceId: currentWorkspaceId + ''},
        {enabled: currentWorkspaceId != null}
    );

    const {data: appsData} = useAppsQuery(
        {environmentId: String(currentEnvironmentId), workspaceId: String(currentWorkspaceId)},
        {enabled: currentWorkspaceId != null && currentEnvironmentId != null}
    );

    const appOptions = useMemo(
        () => (appsData?.apps ?? []).map((app) => ({label: app.name, value: app.id})),
        [appsData]
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
            appOptions={appOptions}
            firstRunEmptyState={<AutomationToolInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={`${currentWorkspaceId}-${currentEnvironmentId}`}
            mcpServerOptions={mcpServerOptions}
            type={PlatformType.Automation}
            workspaceScope={workspaceScope}
        />
    );
};
