import {WorkflowTask} from '@/shared/middleware/platform/configuration';

type NodePositionType = {x: number; y: number};

function getPinnedPosition(task: WorkflowTask | undefined): NodePositionType | undefined {
    const nodePosition = task?.metadata?.ui?.nodePosition;

    if (!nodePosition || typeof nodePosition.x !== 'number' || typeof nodePosition.y !== 'number') {
        return undefined;
    }

    return nodePosition;
}

export default function placeInsertedTaskBetweenPinnedNeighbours(
    tasks: Array<WorkflowTask>,
    insertionIndex: number
): Array<WorkflowTask> {
    const predecessorPosition = getPinnedPosition(tasks[insertionIndex - 1]);
    const successorPosition = getPinnedPosition(tasks[insertionIndex + 1]);

    if (!predecessorPosition || !successorPosition) {
        return tasks;
    }

    const insertedTask = tasks[insertionIndex];

    const placedTask: WorkflowTask = {
        ...insertedTask,
        metadata: {
            ...insertedTask.metadata,
            ui: {
                ...insertedTask.metadata?.ui,
                nodePosition: {
                    x: (predecessorPosition.x + successorPosition.x) / 2,
                    y: (predecessorPosition.y + successorPosition.y) / 2,
                },
            },
        },
    };

    return [...tasks.slice(0, insertionIndex), placedTask, ...tasks.slice(insertionIndex + 1)];
}
