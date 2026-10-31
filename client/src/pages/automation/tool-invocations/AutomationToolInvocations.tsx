import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ToolInvocations} from '@/pages/automation/tool-invocations/ToolInvocations';
import {useWorkspaceMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const AutomationToolInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data} = useWorkspaceMcpServersQuery(
        {workspaceId: currentWorkspaceId + ''},
        {enabled: currentWorkspaceId != null}
    );

    const mcpServerOptions = useMemo(
        () =>
            (data?.workspaceMcpServers ?? [])
                .filter((mcpServer) => mcpServer != null)
                .map((mcpServer) => ({label: mcpServer!.name, value: mcpServer!.id})),
        [data]
    );

    const workspaceScope = useMemo(
        () => ({environmentId: currentEnvironmentId, workspaceId: currentWorkspaceId}),
        [currentEnvironmentId, currentWorkspaceId]
    );

    return <ToolInvocations mcpServerOptions={mcpServerOptions} workspaceScope={workspaceScope} />;
};
