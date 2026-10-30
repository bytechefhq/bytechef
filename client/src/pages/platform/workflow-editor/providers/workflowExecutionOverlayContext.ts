import {TaskExecution} from '@/shared/middleware/platform/workflow/test';
import {createContext, useContext} from 'react';

import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export interface WorkflowExecutionOverlayI {
    isRunning: boolean;
    nodeStates: Record<string, WorkflowTestNodeStateI>;
    taskExecutions: TaskExecution[];
}

export const WorkflowExecutionOverlayContext = createContext<WorkflowExecutionOverlayI | null>(null);

export function useWorkflowExecutionOverlay(): WorkflowExecutionOverlayI | null {
    return useContext(WorkflowExecutionOverlayContext);
}
