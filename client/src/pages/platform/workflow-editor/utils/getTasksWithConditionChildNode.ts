import {WorkflowTask} from '@/shared/middleware/automation/configuration';

interface AddConditionChildNodeProps {
    conditionId: string;
    tasks: Array<WorkflowTask>;
    newTask: WorkflowTask;
    placeholderId: string;
}

export default function getTasksWithConditionChildNode({
    conditionId,
    newTask,
    placeholderId,
    tasks,
}: AddConditionChildNodeProps): Array<WorkflowTask> {
    const conditionTask = tasks.find((task) => task.name === conditionId);

    if (!conditionTask) {
        return tasks;
    }

    const conditionCase = placeholderId?.split('-')[1] === 'left' ? 'true' : 'false';

    if (!conditionTask.parameters) {
        conditionTask.parameters = {
            caseFalse: [],
            caseTrue: [],
        };
    }

    if (conditionCase === 'true') {
        conditionTask.parameters = {
            ...conditionTask.parameters,
            caseTrue: [...conditionTask.parameters.caseTrue, newTask],
        };
    } else {
        conditionTask.parameters = {
            ...conditionTask.parameters,
            caseFalse: [...conditionTask.parameters.caseFalse, newTask],
        };
    }

    return tasks.map((task) => (task.name === conditionId ? conditionTask : task));
}
