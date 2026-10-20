import {GetWorkflowExecutionRequest} from '@/ee/shared/middleware/embedded/workflow/execution';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    WorkflowExecutionKeys,
    useGetIntegrationWorkflowExecutionQuery,
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
    useGetWorkflowExecutionTaskExecutionQuery,
} from '@/ee/shared/queries/embedded/workflowExecutions.queries';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import AutomationWorkflowExecutionDetail from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';
import {
    WorkflowExecutionDetailQueriesI,
    WorkflowExecutionDetailQueriesProvider,
} from '@/shared/components/workflow-executions/providers/workflowExecutionDetailQueriesProvider';

// The embedded models are generated separately from the automation ones but carry the same execution shape, so the
// automation detail view renders them unchanged; only the queries differ.
export const embeddedWorkflowExecutionDetailQueries = {
    taskExecutionChildrenOfJobQueryKey: WorkflowExecutionKeys.workflowExecutionTaskExecutionChildrenOfJob,
    useGetComponentDefinitionsQuery,
    useGetWorkflowExecutionQuery: (
        request: GetWorkflowExecutionRequest,
        enabled: boolean = true,
        refetchInterval?: number | false
    ) => useGetIntegrationWorkflowExecutionQuery(request, enabled, refetchInterval),
    useGetWorkflowExecutionTaskExecutionChildrenInfiniteQuery,
    useGetWorkflowExecutionTaskExecutionQuery,
    useGetWorkflowQuery,
} as unknown as WorkflowExecutionDetailQueriesI;

interface WorkflowExecutionDetailProps {
    enabled?: boolean;
    workflowExecutionId: number;
}

const WorkflowExecutionDetail = ({enabled = true, workflowExecutionId}: WorkflowExecutionDetailProps) => (
    <WorkflowExecutionDetailQueriesProvider value={embeddedWorkflowExecutionDetailQueries}>
        <AutomationWorkflowExecutionDetail embedded enabled={enabled} workflowExecutionId={workflowExecutionId} />
    </WorkflowExecutionDetailQueriesProvider>
);

export default WorkflowExecutionDetail;
