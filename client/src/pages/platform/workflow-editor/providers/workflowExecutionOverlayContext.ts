import {TaskExecution} from '@/shared/middleware/platform/workflow/test';
import {createContext, useContext} from 'react';

import {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

/**
 * A past (or still running) execution drawn over a read-only canvas: which nodes ran and how, the task
 * execution tree graph transitions read their routing history from, and whether the job is still going.
 */
export interface WorkflowExecutionOverlayI {
    isRunning: boolean;
    nodeStates: Record<string, WorkflowTestNodeStateI>;
    taskExecutions: TaskExecution[];
}

/**
 * Supplies node and edge execution state from outside the editor stores. The test-run state lives in a
 * store every canvas shares, so painting a stored execution into it would overwrite the colors of a test
 * run in an open editor; a provider scopes the execution to the canvas beneath it instead. Absent (null),
 * every consumer reads the editor's own test run, exactly as before.
 */
export const WorkflowExecutionOverlayContext = createContext<WorkflowExecutionOverlayI | null>(null);

export function useWorkflowExecutionOverlay(): WorkflowExecutionOverlayI | null {
    return useContext(WorkflowExecutionOverlayContext);
}
