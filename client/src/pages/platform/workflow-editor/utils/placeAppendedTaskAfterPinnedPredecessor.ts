import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {Node} from '@xyflow/react';

interface PlaceAppendedTaskAfterPinnedPredecessorProps {
    canvasNodes: Array<Node>;
    crossAxisShift: number;
    direction: LayoutDirectionType;
    tasks: Array<WorkflowTask>;
}

const PLACEHOLDER_RENDERED_WIDTH = 72;

export default function placeAppendedTaskAfterPinnedPredecessor({
    canvasNodes,
    crossAxisShift,
    direction,
    tasks,
}: PlaceAppendedTaskAfterPinnedPredecessorProps): Array<WorkflowTask> {
    if (tasks.length < 2) {
        return tasks;
    }

    const predecessorPosition = tasks[tasks.length - 2].metadata?.ui?.nodePosition;

    if (
        !predecessorPosition ||
        typeof predecessorPosition.x !== 'number' ||
        typeof predecessorPosition.y !== 'number'
    ) {
        return tasks;
    }

    const trailingPlaceholderNode = canvasNodes.find((node) => node.id === FINAL_PLACEHOLDER_NODE_ID && !node.parentId);

    if (!trailingPlaceholderNode) {
        return tasks;
    }

    const placeholderPosition = trailingPlaceholderNode.position;

    let nodePosition: {x: number; y: number};

    if (direction === 'LR') {
        const placeholderHeight = trailingPlaceholderNode.measured?.height ?? PLACEHOLDER_NODE_HEIGHT;
        const chainLineY = placeholderPosition.y + placeholderHeight / 2;

        nodePosition = {
            x: placeholderPosition.x,
            y: chainLineY - CLUSTER_ELEMENT_NODE_WIDTH / 2 - crossAxisShift,
        };
    } else {
        const placeholderWidth = trailingPlaceholderNode.measured?.width ?? PLACEHOLDER_RENDERED_WIDTH;
        const chainLineX = placeholderPosition.x + placeholderWidth / 2;

        nodePosition = {
            x: chainLineX - NODE_WIDTH / 2 - crossAxisShift,
            y: placeholderPosition.y,
        };
    }

    const appendedTask = tasks[tasks.length - 1];

    const placedTask: WorkflowTask = {
        ...appendedTask,
        metadata: {
            ...appendedTask.metadata,
            ui: {
                ...appendedTask.metadata?.ui,
                nodePosition,
            },
        },
    };

    return [...tasks.slice(0, -1), placedTask];
}
