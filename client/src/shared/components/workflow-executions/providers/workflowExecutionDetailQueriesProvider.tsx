import {Workflow} from '@/shared/middleware/automation/configuration';
import {
    GetWorkflowExecutionRequest,
    GetWorkflowExecutionTaskExecutionRequest,
    TaskExecution,
    TaskExecutionChildrenPage,
    WorkflowExecution,
} from '@/shared/middleware/automation/workflow/execution';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {
    WorkflowExecutionKeys,
    WorkflowExecutionKindType,
    useGetProjectWorkflowExecutionQuery,
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
    useGetWorkflowExecutionTaskExecutionQuery,
} from '@/shared/queries/automation/workflowExecutions.queries';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {GetComponentDefinitionsRequestI} from '@/shared/queries/platform/componentDefinitions.queries';
import {QueryKey, UseInfiniteQueryResult, UseQueryResult} from '@tanstack/react-query';
import {createContext, useContext} from 'react';

/**
 * The queries the workflow execution detail view reads through. The automation queries are the default; embedded
 * pages provide their own so the same detail view, lazy task tree included, serves integration executions.
 */
export interface WorkflowExecutionDetailQueriesI {
    taskExecutionChildrenOfJobQueryKey: (id: number) => QueryKey;
    useGetComponentDefinitionsQuery: (
        request: GetComponentDefinitionsRequestI,
        enabled?: boolean
    ) => UseQueryResult<Array<ComponentDefinitionBasic>, Error>;
    useGetWorkflowExecutionQuery: (
        request: GetWorkflowExecutionRequest,
        enabled?: boolean,
        refetchInterval?: number | false,
        kind?: WorkflowExecutionKindType
    ) => UseQueryResult<WorkflowExecution, Error>;
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery: (
        request: {id: number; taskExecutionId: number},
        enabled: boolean,
        refetchInterval?: number | false,
        initialPageNumber?: number
    ) => UseInfiniteQueryResult<{pages: TaskExecutionChildrenPage[]}, Error>;
    useGetWorkflowExecutionTaskExecutionQuery: (
        request: GetWorkflowExecutionTaskExecutionRequest,
        enabled: boolean,
        refetchInterval?: number | false,
        retry?: boolean
    ) => UseQueryResult<TaskExecution, Error>;
    useGetWorkflowQuery: (id: string, enabled?: boolean) => UseQueryResult<Workflow, Error>;
}

const automationWorkflowExecutionDetailQueries: WorkflowExecutionDetailQueriesI = {
    taskExecutionChildrenOfJobQueryKey: WorkflowExecutionKeys.workflowExecutionTaskExecutionChildrenOfJob,
    useGetComponentDefinitionsQuery,
    useGetWorkflowExecutionQuery: useGetProjectWorkflowExecutionQuery,
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
    useGetWorkflowExecutionTaskExecutionQuery,
    useGetWorkflowQuery,
};

const WorkflowExecutionDetailQueriesContext = createContext<WorkflowExecutionDetailQueriesI>(
    automationWorkflowExecutionDetailQueries
);

export const WorkflowExecutionDetailQueriesProvider = WorkflowExecutionDetailQueriesContext.Provider;

export const useWorkflowExecutionDetailQueries = () => useContext(WorkflowExecutionDetailQueriesContext);
