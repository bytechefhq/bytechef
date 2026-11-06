import {ExternalInvocations} from '@/pages/automation/external-invocations/ExternalInvocations';
import AutomationExternalInvocationsEmptyState from '@/pages/automation/external-invocations/components/AutomationExternalInvocationsEmptyState';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import WorkflowExecutionSheet from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {PlatformType, useA2aServersQuery, useWorkspaceMcpServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useCallback, useMemo} from 'react';

export const AutomationExternalInvocations = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const setWorkflowExecutionId = useWorkflowExecutionSheetStore((state) => state.setWorkflowExecutionId);
    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

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

    const handleViewJob = useCallback(
        (jobId: number) => {
            setWorkflowExecutionId(jobId, 'JOB');
            setWorkflowExecutionSheetOpen(true);
        },
        [setWorkflowExecutionId, setWorkflowExecutionSheetOpen]
    );

    return (
        <ExternalInvocations
            a2aServerOptions={a2aServerOptions}
            firstRunEmptyState={<AutomationExternalInvocationsEmptyState hasMcpServer={hasMcpServer} />}
            key={`${currentWorkspaceId}-${currentEnvironmentId}`}
            mcpServerOptions={mcpServerOptions}
            onViewJob={handleViewJob}
            type={PlatformType.Automation}
            workflowExecutionSheet={<WorkflowExecutionSheet />}
            workspaceScope={workspaceScope}
        />
    );
};
