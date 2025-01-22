import {WorkflowTask} from '@/shared/middleware/platform/configuration';

export default function getParentConditionTask(
    tasks: Array<WorkflowTask>,
    conditionId: string
): WorkflowTask | undefined {
    for (const task of tasks) {
        if (task.name === conditionId) {
            return task;
        }

        if (task.parameters?.caseTrue || task.parameters?.caseFalse) {
            const foundTask =
                getParentConditionTask(task.parameters.caseTrue || [], conditionId) ||
                getParentConditionTask(task.parameters.caseFalse || [], conditionId);

            if (foundTask) {
                return foundTask;
            }
        }
    }

    return undefined;
}
