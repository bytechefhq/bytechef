import {
    GetWorkflowExecutionsPageJobStatusEnum,
    WorkflowExecutionApi as EmbeddedWorkflowExecutionApi,
} from '@/ee/shared/middleware/embedded/workflow/execution';
import {WorkflowExecutionKeys as EmbeddedWorkflowExecutionKeys} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {WorkflowExecutionApi as AutomationWorkflowExecutionApi} from '@/shared/middleware/automation/workflow/execution';
import {useConnectedUserProjectsQuery} from '@/shared/middleware/graphql';
import {Page} from '@/shared/middleware/platform/workflow/execution';
import {WorkflowExecutionKeys as AutomationWorkflowExecutionKeys} from '@/shared/queries/automation/workflowExecutions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useEffect, useState} from 'react';

export const useWorkflowExecutions = (
    automations: number,
    request: {
        environmentId?: number;
        id: number;
        jobStatus?: GetWorkflowExecutionsPageJobStatusEnum;
        jobStartDate?: Date;
        jobEndDate?: Date;
        integrationId?: number;
        integrationInstanceConfigurationId?: number;
        pageNumber?: number;
        projectId?: number;
        workflowId?: string;
    }
) => {
    const [workflowExecutionPage, setWorkflowExecutionPage] = useState<Page | undefined>();
    const [workflowExecutionsError, setWorkflowExecutionsError] = useState<Error | null>(null);
    const [workflowExecutionsIsFetching, setWorkflowExecutionsIsFetching] = useState(false);
    const [workflowExecutionsIsLoading, setWorkflowExecutionsIsLoading] = useState(true);
    const [refetchToken, setRefetchToken] = useState(0);

    const {data: connectedUserProjectsQuery} = useConnectedUserProjectsQuery({
        environmentId: request.environmentId?.toString(),
    });

    const queryClient = useQueryClient();

    const refetchWorkflowExecutions = useCallback(() => setRefetchToken((token) => token + 1), []);

    useEffect(() => {
        setWorkflowExecutionsIsFetching(true);

        const queryKey = automations
            ? AutomationWorkflowExecutionKeys.filteredWorkflowExecutions({...request, embedded: true})
            : EmbeddedWorkflowExecutionKeys.filteredWorkflowExecutions(request);

        if (refetchToken > 0) {
            queryClient.removeQueries({queryKey});
        }

        const queryFn = automations
            ? () => new AutomationWorkflowExecutionApi().getWorkflowExecutionsPage({...request, embedded: true})
            : () => new EmbeddedWorkflowExecutionApi().getWorkflowExecutionsPage(request);

        queryClient
            .fetchQuery({queryFn, queryKey})
            .then((response) => {
                setWorkflowExecutionPage(response);
                setWorkflowExecutionsError(null);
            })
            .catch((error) => setWorkflowExecutionsError(error))
            .finally(() => {
                setWorkflowExecutionsIsFetching(false);
                setWorkflowExecutionsIsLoading(false);
            });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        automations,
        queryClient,
        refetchToken,
        request.environmentId,
        request.integrationId,
        request.integrationInstanceConfigurationId,
        request.jobEndDate,
        request.jobStartDate,
        request.jobStatus,
        request.pageNumber,
        request.projectId,
        request.workflowId,
    ]);

    return {
        connectedUserProjects: connectedUserProjectsQuery?.connectedUserProjects,
        refetchWorkflowExecutions,
        workflowExecutionPage,
        workflowExecutionsError,
        workflowExecutionsIsFetching,
        workflowExecutionsIsLoading,
    };
};
