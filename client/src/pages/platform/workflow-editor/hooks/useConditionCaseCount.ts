import collectConditionCaseCounts from '../edges/collectConditionCaseCounts';
import {type ConditionCaseTaskExecutionI, toConditionCaseKey} from '../edges/collectTakenEmptyConditionCases';
import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';

const conditionCaseCountsByTaskExecutions = new WeakMap<ConditionCaseTaskExecutionI[], Map<string, number>>();

function getConditionCaseCounts(taskExecutions: ConditionCaseTaskExecutionI[]): Map<string, number> {
    let conditionCaseCounts = conditionCaseCountsByTaskExecutions.get(taskExecutions);

    if (!conditionCaseCounts) {
        conditionCaseCounts = collectConditionCaseCounts(taskExecutions);

        conditionCaseCountsByTaskExecutions.set(taskExecutions, conditionCaseCounts);
    }

    return conditionCaseCounts;
}

export default function useConditionCaseCount(conditionName: string, conditionCase: string): number {
    const testTaskExecutions = useWorkflowEditorStore((state) => state.workflowTestExecution?.job?.taskExecutions);

    const workflowExecutionOverlay = useWorkflowExecutionOverlay();

    const taskExecutions = workflowExecutionOverlay ? workflowExecutionOverlay.taskExecutions : testTaskExecutions;

    if (!taskExecutions) {
        return 0;
    }

    return getConditionCaseCounts(taskExecutions).get(toConditionCaseKey(conditionName, conditionCase)) ?? 0;
}
