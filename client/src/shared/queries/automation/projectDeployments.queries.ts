/* eslint-disable sort-keys */
import {
    GetWorkspaceProjectDeploymentsRequest,
    ProjectDeployment,
    ProjectDeploymentApi,
} from '@/shared/middleware/automation/configuration';
import {useQuery} from '@tanstack/react-query';

export const ProjectDeploymentKeys = {
    filteredProjectDeployments: (filters: GetWorkspaceProjectDeploymentsRequest) => [
        ...ProjectDeploymentKeys.projectDeployments,
        filters.id,
        filters,
    ],
    projectDeployment: (id: number) => [...ProjectDeploymentKeys.projectDeployments, id],
    projectDeployments: ['projectDeployments'] as const,
};

export const useGetProjectDeploymentQuery = (id: number, enabled?: boolean) =>
    useQuery<ProjectDeployment, Error>({
        queryKey: ProjectDeploymentKeys.projectDeployment(id),
        queryFn: () => new ProjectDeploymentApi().getProjectDeployment({id}),
        enabled: enabled === undefined ? true : enabled,
    });

const RUNNING_EXECUTION_REFETCH_INTERVALS = [
    {interval: 3000, maxAge: 60 * 1000},
    {interval: 10 * 1000, maxAge: 10 * 60 * 1000},
    {interval: 30 * 1000, maxAge: 60 * 60 * 1000},
];

interface ExecutionDatesI {
    lastExecutionDate?: Date;
    lastExecutionStartDate?: Date;
}

const getRunningExecutionStartTime = (execution: ExecutionDatesI) =>
    execution.lastExecutionStartDate && !execution.lastExecutionDate
        ? execution.lastExecutionStartDate.getTime()
        : undefined;

export const getProjectDeploymentsRefetchInterval = (
    projectDeployments?: ProjectDeployment[],
    now: number = Date.now()
): number | false => {
    const runningExecutionStartTimes = (projectDeployments ?? [])
        .flatMap((projectDeployment) => [projectDeployment, ...(projectDeployment.projectDeploymentWorkflows ?? [])])
        .map(getRunningExecutionStartTime)
        .filter((startTime): startTime is number => startTime !== undefined);

    if (!runningExecutionStartTimes.length) {
        return false;
    }

    const youngestRunningExecutionAge = now - Math.max(...runningExecutionStartTimes);

    const refetchInterval = RUNNING_EXECUTION_REFETCH_INTERVALS.find(
        ({maxAge}) => youngestRunningExecutionAge < maxAge
    );

    return refetchInterval?.interval ?? false;
};

export const useGetWorkspaceProjectDeploymentsQuery = (
    filters: GetWorkspaceProjectDeploymentsRequest,
    enabled?: boolean
) =>
    useQuery<ProjectDeployment[], Error>({
        queryKey: ProjectDeploymentKeys.filteredProjectDeployments(filters),
        queryFn: () => new ProjectDeploymentApi().getWorkspaceProjectDeployments(filters),
        enabled: enabled === undefined ? true : enabled,
        refetchInterval: (query) => getProjectDeploymentsRefetchInterval(query.state.data),
    });
