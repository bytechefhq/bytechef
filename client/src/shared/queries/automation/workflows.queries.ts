import {Workflow, WorkflowApi} from '@/shared/middleware/automation/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const WorkflowKeys = {
    workflow: (id: string) => [...WorkflowKeys.workflows, id],
    workflowByWorkflowExecutionId: (workflowExecutionId: string) => [
        ...WorkflowKeys.workflows,
        'execution',
        workflowExecutionId,
    ],
    workflows: ['automationWorkflows'],
};

export const useGetWorkflowQuery = (id: string, enabled?: boolean) =>
    useQuery<Workflow, Error>({
        queryKey: WorkflowKeys.workflow(id),
        queryFn: () => new WorkflowApi().getWorkflow({id}),
        enabled: enabled === undefined ? true : enabled,
    });
