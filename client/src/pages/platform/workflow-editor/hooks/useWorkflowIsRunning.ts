import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

/**
 * Whether the run drawn on this canvas is still going, which animates its edges. Under a
 * `WorkflowExecutionOverlayContext` provider that is the overlaid execution's job, not the editor's test
 * run — otherwise a test started in the editor would animate every read-only canvas as well.
 */
export default function useWorkflowIsRunning(): boolean {
    const workflowIsRunning = useWorkflowEditorStore((state) => state.workflowIsRunning);

    const workflowExecutionOverlay = useWorkflowExecutionOverlay();

    return workflowExecutionOverlay ? workflowExecutionOverlay.isRunning : workflowIsRunning;
}
