import {useMemo} from 'react';

import collectTakenEmptyConditionCases from '../edges/collectTakenEmptyConditionCases';
import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

const NO_TAKEN_EMPTY_CONDITION_CASES = new Set<string>();

/**
 * The empty condition cases the displayed run took (see `collectTakenEmptyConditionCases`), read from the stored
 * execution under a read-only overlay and from the editor's last test run otherwise.
 *
 * `enabled` is false for anything that is not a condition's plumbing: those callers get a constant empty set and
 * do not subscribe to the run's execution tree, which a streaming test run replaces on every progress snapshot.
 */
export default function useTakenEmptyConditionCases(enabled: boolean): Set<string> {
    const testTaskExecutions = useWorkflowEditorStore((state) =>
        enabled ? state.workflowTestExecution?.job?.taskExecutions : undefined
    );

    const workflowExecutionOverlay = useWorkflowExecutionOverlay();

    const taskExecutions = workflowExecutionOverlay ? workflowExecutionOverlay.taskExecutions : testTaskExecutions;

    return useMemo(
        () => (enabled ? collectTakenEmptyConditionCases(taskExecutions) : NO_TAKEN_EMPTY_CONDITION_CASES),
        [enabled, taskExecutions]
    );
}
