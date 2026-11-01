import EmbeddedToolInvocationsEmptyState from '@/ee/pages/embedded/tool-invocations/components/EmbeddedToolInvocationsEmptyState';
import {ToolInvocations} from '@/pages/automation/tool-invocations/ToolInvocations';
import {PlatformType, useEmbeddedMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const EmbeddedToolInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data} = useEmbeddedMcpServersQuery();

    const hasMcpServer = useMemo(
        () =>
            (data?.embeddedMcpServers ?? []).some(
                (mcpServer) => mcpServer != null && mcpServer.environmentId === String(currentEnvironmentId)
            ),
        [currentEnvironmentId, data]
    );

    return (
        <ToolInvocations
            environmentId={currentEnvironmentId}
            firstRunEmptyState={<EmbeddedToolInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={currentEnvironmentId}
            type={PlatformType.Embedded}
        />
    );
};
