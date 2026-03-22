import {ON_ERROR_WIRE_KEY_ERROR_BRANCH, ON_ERROR_WIRE_KEY_MAIN_BRANCH} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';

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

            if (task.type.split('/')[0] === 'on-error' && task.parameters) {
                const mainBranch = task.parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH];
                const errorBranch = task.parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH];

                if (Array.isArray(mainBranch)) {
                    names.push(...extractTaskNames(mainBranch));
                }

                if (Array.isArray(errorBranch)) {
                    names.push(...extractTaskNames(errorBranch));
                }
            }

            return names;
        });
    };

    return extractTaskNames(tasks);
}
