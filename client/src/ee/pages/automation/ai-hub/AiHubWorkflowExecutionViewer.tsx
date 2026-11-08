import WorkflowExecutionDetail from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';

interface AiHubWorkflowExecutionViewerPropsI {
    workflowExecutionId: number;
}

const AiHubWorkflowExecutionViewer = ({workflowExecutionId}: AiHubWorkflowExecutionViewerPropsI) => (
    <div className="flex size-full flex-col p-2">
        <WorkflowExecutionDetail workflowExecutionId={workflowExecutionId} />
    </div>
);

export default AiHubWorkflowExecutionViewer;
