import {WorkflowTestConfiguration, WorkflowTestConfigurationApi} from '@/shared/middleware/platform/configuration';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useQueries} from '@tanstack/react-query';

interface LatestWorkflowI {
    id?: string;
    workflowUuid?: string;
}

export interface WorkflowTestConfigurationConnectionsI {
    findConnectionId: (
        workflowUuid: string | undefined,
        workflowNodeName: string,
        workflowConnectionKey: string
    ) => number | undefined;
    isFetched: boolean;
}

export default function useWorkflowTestConfigurationConnections(
    latestWorkflows: LatestWorkflowI[] | undefined,
    environmentId: number | undefined,
    enabled: boolean
): WorkflowTestConfigurationConnectionsI {
    const workflowsWithIds = (latestWorkflows ?? []).filter(
        (workflow): workflow is Required<LatestWorkflowI> => !!workflow.id && !!workflow.workflowUuid
    );

    const testConfigurationQueries = useQueries({
        queries: workflowsWithIds.map((workflow) => ({
            enabled: enabled && environmentId != null,
            queryFn: () =>
                new WorkflowTestConfigurationApi().getWorkflowTestConfiguration({
                    environmentId: environmentId!,
                    workflowId: workflow.id,
                }),
            queryKey: [...WorkflowTestConfigurationKeys.workflowTestConfiguration(workflow.id), environmentId],
            retry: false,
        })),
    });

    const findConnectionId = (
        workflowUuid: string | undefined,
        workflowNodeName: string,
        workflowConnectionKey: string
    ) => {
        const workflowIndex = workflowsWithIds.findIndex((workflow) => workflow.workflowUuid === workflowUuid);

        if (workflowIndex < 0) {
            return undefined;
        }

        const testConfiguration = testConfigurationQueries[workflowIndex]?.data as
            WorkflowTestConfiguration | null | undefined;

        return testConfiguration?.connections?.find(
            (connection) =>
                connection.workflowNodeName === workflowNodeName &&
                connection.workflowConnectionKey === workflowConnectionKey
        )?.connectionId;
    };

    return {
        findConnectionId,
        isFetched:
            enabled && latestWorkflows !== undefined && testConfigurationQueries.every((query) => query.isFetched),
    };
}
