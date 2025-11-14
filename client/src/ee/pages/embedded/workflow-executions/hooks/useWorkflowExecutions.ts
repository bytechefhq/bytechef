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
import {useEffect, useState} from 'react';

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
    const [workflowExecutionsIsLoading, setWorkflowExecutionsIsLoading] = useState(true);

    const {data: connectedUserProjectsQuery} = useConnectedUserProjectsQuery({
        environmentId: request.environmentId?.toString(),
    });

    const queryClient = useQueryClient();

    useEffect(() => {
        if (automations) {
            queryClient
                .fetchQuery({
                    queryFn: () =>
                        new AutomationWorkflowExecutionApi().getWorkflowExecutionsPage({
                            ...request,
                            embedded: true,
                        }),
                    queryKey: AutomationWorkflowExecutionKeys.filteredWorkflowExecutions({
                        ...request,
                        embedded: true,
                    }),
                })
                .then((response) => {
                    setWorkflowExecutionsIsLoading(false);
                    setWorkflowExecutionPage(response);
                })
                .catch((error) => {
                    setWorkflowExecutionsError(error);
                    setWorkflowExecutionsIsLoading(false);
                });
        } else {
            queryClient
                .fetchQuery({
                    queryFn: () => new EmbeddedWorkflowExecutionApi().getWorkflowExecutionsPage(request),
                    queryKey: EmbeddedWorkflowExecutionKeys.filteredWorkflowExecutions(request),
                })
                .then((response) => {
                    setWorkflowExecutionsIsLoading(false);
                    setWorkflowExecutionPage(response);
                })
                .catch((error) => {
                    setWorkflowExecutionsError(error);
                    setWorkflowExecutionsIsLoading(false);
                });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [
        automations,
        queryClient,
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
        workflowExecutionPage,
        workflowExecutionsError,
        workflowExecutionsIsLoading,
    };
};
