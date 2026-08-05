import WorkflowExecutionDetail from '@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail';

interface AiHubWorkflowExecutionViewerProps {
    workflowExecutionId: number;
}

/**
 * Body of a `workflowExecution` resource tab. Renders the shared {@link WorkflowExecutionDetail}
 * (job / trigger / task accordion + read-only canvas) — the same content as the standalone
 * workflow-execution slide-over sheet, minus the Sheet chrome.
 *
 * <p>No internal title bar — the tab strip in {@link AiHubResourcePanel} already shows the
 * execution name; a duplicate header here would just push the canvas down.</p>
 *
 * <p>The wrapper is a flex-col so {@link WorkflowExecutionDetail}'s own {@code flex-1 min-h-0}
 * root expands to full panel height — matches how the standalone {@code WorkflowExecutionSheet}
 * mounts the detail directly inside a flex-col parent (an intermediate block wrapper would
 * sever the flex chain and the canvas would collapse to its content height).</p>
 *
 * <p>Extra {@code p-2} on top of {@link WorkflowExecutionDetail}'s own {@code p-3} gives the
 * accordion + canvas a bit more breathing room from the tab strip / panel edges. Layered here
 * (not on the shared detail) so the standalone sheet's tighter spacing stays unchanged.</p>
 */
const AiHubWorkflowExecutionViewer = ({workflowExecutionId}: AiHubWorkflowExecutionViewerProps) => (
    <div className="flex size-full flex-col p-2">
        <WorkflowExecutionDetail workflowExecutionId={workflowExecutionId} />
    </div>
);

export default AiHubWorkflowExecutionViewer;
