import {TASK_DISPATCHER_SUBTASK_COLLECTIONS} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

type GetParentTaskType = {
    tasks: Array<WorkflowTask>;
    workflowNodeName: string;
};

const ALL_COLLECTION_NAMES = Object.values(TASK_DISPATCHER_SUBTASK_COLLECTIONS).flat();

export function getTask({tasks, workflowNodeName}: GetParentTaskType): WorkflowTask | undefined {
    // Prefer a direct match at the current level before recursing.
    // Server-flattened DTOs (with .connections, .clusterElements, .clusterRoot populated)
    // coexist with raw nested maps embedded in task dispatcher parameters; recursing first
    // would return the raw nested copy and shadow the enriched DTO.
    const directMatch = tasks.find((task) => task?.name === workflowNodeName);

    if (directMatch) {
        return directMatch;
    }

    for (const task of tasks) {
        if (!task.parameters) {
            continue;
        }

        for (const collectionName of ALL_COLLECTION_NAMES) {
            let subtasks = task.parameters[collectionName];

            if (collectionName === 'cases') {
                subtasks = subtasks?.flatMap((branchCase: BranchCaseType) => branchCase.tasks);
            } else if (collectionName === 'branches') {
                subtasks = subtasks?.flat();
            } else if (collectionName === 'iteratee') {
                if (subtasks && typeof subtasks === 'object' && !Array.isArray(subtasks)) {
                    subtasks = [subtasks];
                } else if (!Array.isArray(subtasks)) {
                    subtasks = [];
                }
            }

            if (Array.isArray(subtasks) && subtasks.length > 0) {
                const foundTask = getTask({tasks: subtasks, workflowNodeName: workflowNodeName});

                if (foundTask) {
                    return foundTask;
                }
            }
        }
    }

    return undefined;
}
