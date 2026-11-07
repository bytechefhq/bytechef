import {
    WorkflowExecutionsSource,
    WorkflowExecutionsSourceType,
} from '@/ee/pages/embedded/workflow-executions/utils/workflowExecutionsSources';
import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionApi as EmbeddedWorkflowExecutionApi,
} from '@/ee/shared/middleware/embedded/workflow/execution';
import {WorkflowExecutionKeys as EmbeddedWorkflowExecutionKeys} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {WorkflowExecutionApi as AutomationWorkflowExecutionApi} from '@/shared/middleware/automation/workflow/execution';
import {useConnectedUserProjectsQuery} from '@/shared/middleware/graphql';
import {Page} from '@/shared/middleware/platform/workflow/execution';
import {WorkflowExecutionKeys as AutomationWorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQuery} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';

const getAnyWorkflowExecutionsPageRequest = (request: {
    connectedUserId?: number;
    environmentId?: number;
    id: number;
    jobEndDate?: Date;
    jobStartDate?: Date;
    jobStatus?: GetWorkflowExecutionsPageJobStatusEnum;
    pageNumber?: number;
}) => ({
    connectedUserId: request.connectedUserId,
    environmentId: request.environmentId,
    jobEndDate: request.jobEndDate,
    jobStartDate: request.jobStartDate,
    jobStatus: request.jobStatus,
    pageNumber: request.pageNumber,
    workspaceId: request.id,
});

export const useWorkflowExecutions = (
    source: WorkflowExecutionsSourceType,
    request: {
        connectedUserId?: number;
        environmentId?: number;
        id: number;
        jobStatus?: GetWorkflowExecutionsPageJobStatusEnum;
        jobStartDate?: Date;
        jobEndDate?: Date;
        integrationId?: number;
        integrationInstanceConfigurationId?: number;
        pageNumber?: number;
        workflowId?: string;
    }
) => {
    const {data: connectedUserProjectsQuery} = useConnectedUserProjectsQuery({
        environmentId: request.environmentId?.toString(),
    });

    const {queryFn, queryKey} = useMemo(() => {
        if (source === WorkflowExecutionsSource.Any) {
            const anyRequest = getAnyWorkflowExecutionsPageRequest(request);

            return {
                queryFn: (): Promise<Page> =>
                    new EmbeddedWorkflowExecutionApi().getAnyWorkflowExecutionsPage(anyRequest),
                queryKey: EmbeddedWorkflowExecutionKeys.filteredAnyWorkflowExecutions(anyRequest),
            };
        }

        if (source === WorkflowExecutionsSource.Automations) {
            return {
                queryFn: (): Promise<Page> =>
                    new AutomationWorkflowExecutionApi().getWorkflowExecutionsPage({...request, embedded: true}),
                queryKey: AutomationWorkflowExecutionKeys.filteredWorkflowExecutions({...request, embedded: true}),
            };
        }

        return {
            queryFn: (): Promise<Page> => new EmbeddedWorkflowExecutionApi().getWorkflowExecutionsPage(request),
            queryKey: EmbeddedWorkflowExecutionKeys.filteredWorkflowExecutions(request),
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        request.connectedUserId,
        request.environmentId,
        request.id,
        request.integrationId,
        request.integrationInstanceConfigurationId,
        request.jobEndDate,
        request.jobStartDate,
        request.jobStatus,
        request.pageNumber,
        request.workflowId,
        source,
    ]);

    const {
        data: workflowExecutionPage,
        error: workflowExecutionsError,
        isFetching: workflowExecutionsIsFetching,
        isLoading: workflowExecutionsIsLoading,
        refetch,
    } = useQuery<Page, Error>({queryFn, queryKey});

    const refetchWorkflowExecutions = useCallback(() => {
        void refetch();
    }, [refetch]);

    return {
        connectedUserProjects: connectedUserProjectsQuery?.connectedUserProjects,
        refetchWorkflowExecutions,
        workflowExecutionPage,
        workflowExecutionsError,
        workflowExecutionsIsFetching,
        workflowExecutionsIsLoading,
    };
};
