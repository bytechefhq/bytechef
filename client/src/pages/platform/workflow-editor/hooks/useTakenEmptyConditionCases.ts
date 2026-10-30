import {useMemo} from 'react';

import collectTakenEmptyConditionCases from '../edges/collectTakenEmptyConditionCases';
import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

const NO_TAKEN_EMPTY_CONDITION_CASES = new Set<string>();

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
