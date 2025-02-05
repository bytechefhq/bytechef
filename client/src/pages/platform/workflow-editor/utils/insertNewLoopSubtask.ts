import {WorkflowTask} from '@/shared/middleware/automation/configuration';

interface InsertLoopSubtasksProps {
    loopId: string;
    newTask: WorkflowTask;
    placeholderId: string;
    tasks: Array<WorkflowTask>;
}

export default function insertNewLoopSubtask({
    loopId,
    newTask,
    placeholderId,
    tasks,
}: InsertLoopSubtasksProps): Array<WorkflowTask> {
    const loopTask = tasks.find((task) => task.name === loopId);

    if (!loopTask) {
        return tasks;
    }

    if (!loopTask.parameters?.iteratee) {
        loopTask.parameters!.iteratee = [];
    }

    const taskIndex = parseInt(placeholderId.split('-').pop() || '-1');

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
