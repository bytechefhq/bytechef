import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {getNestedBottomGhostId, getWorkflowNodeComponentName} from './workflowNodeNameUtils';

/**
 * Creates edges for the left ghost node in a parallel task
 */
function createEdgesForLeftGhost(parallelId: string): Edge[] {
    const topGhostId = `${parallelId}-parallel-top-ghost`;
    const leftGhostId = `${parallelId}-taskDispatcher-left-ghost`;
    const bottomGhostId = `${parallelId}-parallel-bottom-ghost`;

    const edgeFromTopGhostToLeftGhost = {
        id: `${topGhostId}=>${leftGhostId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-left`,
        style: EDGE_STYLES,
        target: leftGhostId,
        type: 'smoothstep',
    };

    const edgeFromLeftGhostToBottomGhost = {
        id: `${leftGhostId}=>${bottomGhostId}`,
        source: leftGhostId,
        style: EDGE_STYLES,
        target: bottomGhostId,
        targetHandle: `${bottomGhostId}-left`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToLeftGhost, edgeFromLeftGhostToBottomGhost];
}

/**
 * Creates edges for the placeholder node in a parallel task
 */
function createEdgesForPlaceholder(parallelId: string): Edge[] {
    const topGhostId = `${parallelId}-parallel-top-ghost`;
    const bottomGhostId = `${parallelId}-parallel-bottom-ghost`;
    const placeholderId = `${parallelId}-parallel-placeholder-0`;

    const edgeFromTopGhostToPlaceholder = {
        id: `${topGhostId}=>${placeholderId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-right`,
        style: EDGE_STYLES,
        target: placeholderId,
        type: 'smoothstep',
    };

    const edgeFromPlaceholderToBottomGhost = {
        id: `${placeholderId}=>${bottomGhostId}`,
        source: placeholderId,
        style: EDGE_STYLES,
        target: bottomGhostId,
        targetHandle: `${bottomGhostId}-right`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToPlaceholder, edgeFromPlaceholderToBottomGhost];
}

/**
 * Creates edges connecting a task to a parallel node's ghosts
 */
function createParallelTaskEdges(
    parallelId: string,
    task: WorkflowTask,
    position: 'left' | 'middle' | 'right'
): Edge[] {
    const taskEdges: Edge[] = [];
    const taskId = task.name;
    const taskComponentName = getWorkflowNodeComponentName(taskId);
    const topGhostId = `${parallelId}-parallel-top-ghost`;
    const bottomGhostId = `${parallelId}-parallel-bottom-ghost`;

    const topGhostHandlePosition = position === 'middle' ? 'bottom' : position;
    const bottomGhostHandlePosition = position === 'middle' ? 'top' : position;

    const edgeFromTopGhostToTask = {
        id: `${topGhostId}=>${taskId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
        style: EDGE_STYLES,
        target: taskId,
        type: 'smoothstep',
    };

    taskEdges.push(edgeFromTopGhostToTask);

    if (
        TASK_DISPATCHER_NAMES.includes(taskComponentName) &&
        !CHILDLESS_TASK_DISPATCHER_NAMES.includes(taskComponentName)
    ) {
        const nestedBottomGhostId = getNestedBottomGhostId(taskId);

        const edgeFromNestedGhostToBottomGhost = {
            id: `${nestedBottomGhostId}=>${bottomGhostId}`,
            source: nestedBottomGhostId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: 'smoothstep',
        };

        taskEdges.push(edgeFromNestedGhostToBottomGhost);
    } else {
        const edgeFromTaskToBottomGhost = {
            id: `${taskId}=>${bottomGhostId}`,
            source: taskId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: 'smoothstep',
        };

        taskEdges.push(edgeFromTaskToBottomGhost);
    }

    return taskEdges;
}

/**
 * Distributes parallel branches into left, middle, and right groups
 */
function distributeBranches(tasks: WorkflowTask[]): {
    leftBranches: WorkflowTask[];
    middleBranch: WorkflowTask | null;
    rightBranches: WorkflowTask[];
} {
    if (tasks.length === 1) {
        return {leftBranches: [], middleBranch: null, rightBranches: tasks};
    }

    const isEvenCount = tasks.length % 2 === 0;

    if (isEvenCount) {
        const halfPoint = tasks.length / 2;

        return {
            leftBranches: tasks.slice(0, halfPoint),
            middleBranch: null,
            rightBranches: tasks.slice(halfPoint),
        };
    } else {
        const middleIndex = Math.floor(tasks.length / 2);

        return {
            leftBranches: tasks.slice(0, middleIndex),
            middleBranch: tasks[middleIndex],
            rightBranches: tasks.slice(middleIndex + 1),
        };
    }
}

function markAddBranchEdge(edges: Edge[], lastLaneEntryEdgeId: string, parallelId: string): void {
    const lastLaneEntryEdge = edges.find((edge) => edge.id === lastLaneEntryEdgeId);

    if (lastLaneEntryEdge) {
        lastLaneEntryEdge.data = {
            ...lastLaneEntryEdge.data,
            addBranchPlaceholderId: `${parallelId}-parallel-placeholder-0`,
        };
    }
}

/**
 * Creates all edges for a parallel node and its branches
 */
export default function createParallelEdges(parallelNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = parallelNode.data as NodeDataType;
    const parallelId = parallelNode.id;

    edges.push({
        id: `${parallelId}=>${parallelId}-parallel-top-ghost`,
        source: parallelId,
        style: EDGE_STYLES,
        target: `${parallelId}-parallel-top-ghost`,
        type: 'smoothstep',
    });

    const laneCount = nodeData.parameters?.tasks?.length ?? 0;

    if (laneCount <= 1) {
        const leftGhostEdges = createEdgesForLeftGhost(parallelId);

        edges.push(...leftGhostEdges);
    }

    if (laneCount > 0) {
        const parallelTasks: WorkflowTask[] = nodeData.parameters?.tasks;

        const {leftBranches, middleBranch, rightBranches} = distributeBranches(parallelTasks);

        leftBranches.forEach((task) => {
            const taskEdges = createParallelTaskEdges(parallelId, task, 'left');
            edges.push(...taskEdges);
        });

        if (middleBranch) {
            const taskEdges = createParallelTaskEdges(parallelId, middleBranch, 'middle');
            edges.push(...taskEdges);
        }

        rightBranches.forEach((task) => {
            const taskEdges = createParallelTaskEdges(parallelId, task, 'right');
            edges.push(...taskEdges);
        });

        const lastTaskName = parallelTasks[parallelTasks.length - 1].name;

        markAddBranchEdge(edges, `${parallelId}-parallel-top-ghost=>${lastTaskName}`, parallelId);
    }

    const placeholderEdges = createEdgesForPlaceholder(parallelNode.id);

    edges.push(...placeholderEdges);

    return edges;
}
