import {WorkflowTask} from '@/shared/middleware/automation/configuration';

export default function getAllTaskNames(tasks: Array<WorkflowTask>): Array<string> {
    const extractTaskNames = (taskList: Array<WorkflowTask>): Array<string> => {
        return taskList.flatMap((task) => {
            const names = [task.name];

            if (task.type.includes('condition/')) {
                if (task.parameters?.caseTrue) {
                    names.push(...extractTaskNames(task.parameters.caseTrue));
                }

                if (task.parameters?.caseFalse) {
                    names.push(...extractTaskNames(task.parameters.caseFalse));
                }
            }

            return names;
        });
    };

    return extractTaskNames(tasks);
}
