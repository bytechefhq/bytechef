import EmbeddedExternalInvocationsEmptyState from '@/ee/pages/embedded/external-invocations/components/EmbeddedExternalInvocationsEmptyState';
import {ExternalInvocations} from '@/pages/automation/external-invocations/ExternalInvocations';
import {PlatformType, useEmbeddedMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export const EmbeddedExternalInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data} = useEmbeddedMcpServersQuery();

    const mcpServerOptions = useMemo(
        () =>
            (data?.embeddedMcpServers ?? [])
                .filter((mcpServer) => mcpServer != null)
                .map((mcpServer) => ({label: mcpServer!.name, value: mcpServer!.id})),
        [data]
    );

    const hasMcpServer = useMemo(
        () =>
            (data?.embeddedMcpServers ?? []).some(
                (mcpServer) => mcpServer != null && mcpServer.environmentId === String(currentEnvironmentId)
            ),
        [currentEnvironmentId, data]
    );

    return (
        <ExternalInvocations
            environmentId={currentEnvironmentId}
            firstRunEmptyState={<EmbeddedExternalInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={currentEnvironmentId}
            mcpServerOptions={mcpServerOptions}
            type={PlatformType.Embedded}
        />
    );
};
