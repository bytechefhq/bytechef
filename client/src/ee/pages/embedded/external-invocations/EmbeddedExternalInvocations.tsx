import EmbeddedExternalInvocationsEmptyState from '@/ee/pages/embedded/external-invocations/components/EmbeddedExternalInvocationsEmptyState';
import WorkflowExecutionSheet from '@/ee/pages/embedded/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import useWorkflowExecutionSheetStore from '@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {ExternalInvocations} from '@/pages/automation/external-invocations/ExternalInvocations';
import {PlatformType, useEmbeddedMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useCallback, useMemo} from 'react';

export const EmbeddedExternalInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setWorkflowExecutionId = useWorkflowExecutionSheetStore((state) => state.setWorkflowExecutionId);
    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

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

    const handleViewJob = useCallback(
        (jobId: number) => {
            setWorkflowExecutionId(jobId);
            setWorkflowExecutionSheetOpen(true);
        },
        [setWorkflowExecutionId, setWorkflowExecutionSheetOpen]
    );

    return (
        <ExternalInvocations
            environmentId={currentEnvironmentId}
            firstRunEmptyState={<EmbeddedExternalInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={currentEnvironmentId}
            mcpServerOptions={mcpServerOptions}
            onViewJob={handleViewJob}
            type={PlatformType.Embedded}
            workflowExecutionSheet={<WorkflowExecutionSheet />}
        />
    );
};
