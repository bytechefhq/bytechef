import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

export default function useWorkflowIsRunning(): boolean {
    const workflowIsRunning = useWorkflowEditorStore((state) => state.workflowIsRunning);

    const workflowExecutionOverlay = useWorkflowExecutionOverlay();

    return workflowExecutionOverlay ? workflowExecutionOverlay.isRunning : workflowIsRunning;
}
