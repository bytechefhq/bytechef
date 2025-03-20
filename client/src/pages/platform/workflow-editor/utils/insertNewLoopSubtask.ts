import {WorkflowTask} from '@/shared/middleware/automation/configuration';
import {TaskDispatcherContextType} from '@/shared/types';

import getParentLoopTask from './getParentLoopTask';

interface InsertLoopSubtasksProps {
    loopId: string;
    newTask: WorkflowTask;
    placeholderId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    tasks: Array<WorkflowTask>;
}

export default function insertNewLoopSubtask({
    loopId,
    newTask,
    placeholderId,
    taskDispatcherContext,
    tasks,
}: InsertLoopSubtasksProps): Array<WorkflowTask> {
    let taskIndex: number | undefined;

    let loopTask = tasks.find((task) => task.name === loopId);

    if (!loopTask) {
        loopTask = getParentLoopTask(tasks, loopId);
    }

    if (!loopTask) {
        return tasks;
    }

    if (!loopTask) {
        return tasks;
    }

    if (!loopTask.parameters?.iteratee) {
        loopTask.parameters!.iteratee = [];
    }

    if (taskDispatcherContext?.loopId === loopId && taskDispatcherContext.index !== undefined) {
        taskIndex = taskDispatcherContext.index;
    } else if (placeholderId) {
        taskIndex = parseInt(placeholderId.split('-').pop() || '-1');
    }

    if (taskIndex === undefined || taskIndex === -1 || typeof taskIndex !== 'number') {
        loopTask.parameters = {
            ...loopTask.parameters,
            iteratee: [...(loopTask.parameters?.iteratee || []), newTask],
        };
    } else {
        const iteratee = [...(loopTask.parameters?.iteratee || [])];

        iteratee.splice(taskIndex, 0, newTask);

        loopTask.parameters = {
            ...loopTask.parameters,
            iteratee,
        };
    }

    return tasks.map((task) => (task.name === loopId ? loopTask : task));
}
