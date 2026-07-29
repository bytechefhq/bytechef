import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ToolInvocations} from '@/pages/automation/tool-invocations/ToolInvocations';
import {useWorkspaceMcpServersQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export const AutomationToolInvocations = () => {
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

    return <ToolInvocations basePath="/automation/executions" mcpServerOptions={mcpServerOptions} />;
};
