import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates the base loop structure edges (loop -> top ghost -> left ghost -> bottom ghost)
 */
function createBaseLoopStructureEdges(loopId: string): Edge[] {
    const topGhostId = `${loopId}-loop-top-ghost`;
    const bottomGhostId = `${loopId}-loop-bottom-ghost`;
    const leftGhostId = `${loopId}-taskDispatcher-left-ghost`;

    const edgeFromLoopToTopGhost = {
        id: `${loopId}=>${topGhostId}`,
        source: loopId,
        style: EDGE_STYLES,
        target: topGhostId,
        type: 'smoothstep',
    };

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

    return [edgeFromLoopToTopGhost, edgeFromTopGhostToLeftGhost, edgeFromLeftGhostToBottomGhost];
}

/**
 * Creates edges for empty loop (with placeholder)
 */
function createEdgesForEmptyLoop(loopId: string): Edge[] {
    const topGhostId = `${loopId}-loop-top-ghost`;
    const bottomGhostId = `${loopId}-loop-bottom-ghost`;
    const placeholderId = `${loopId}-loop-placeholder-0`;

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
 * Creates edges between loop tasks
 */
function createLoopSubtaskEdges(loopId: string, loopChildTasks: Array<WorkflowTask>): Edge[] {
    const edges: Edge[] = [];

    const topGhostId = `${loopId}-loop-top-ghost`;
    const bottomGhostId = `${loopId}-loop-bottom-ghost`;

    if (loopChildTasks.length === 0) {
        return [];
    }

    const edgeFromTopGhostToFirstLoopChildTask = {
        id: `${topGhostId}=>${loopChildTasks[0].name}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-right`,
        style: EDGE_STYLES,
        target: loopChildTasks[0].name,
        type: 'workflow',
    };

    edges.push(edgeFromTopGhostToFirstLoopChildTask);

    loopChildTasks.forEach((task, index) => {
        const sourceTaskName = task.name;
        const sourceTaskComponentName = task.name.split('_')[0];

        const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName);
        const isLeafTaskDispatcher = CHILDLESS_TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName);
        const isLastTask = index === loopChildTasks.length - 1;

        let targetId;
        let targetHandleId;

        if (isLastTask) {
            targetId = bottomGhostId;
            targetHandleId = `${bottomGhostId}-right`;
        } else {
            targetId = loopChildTasks[index + 1].name;
            targetHandleId = undefined;
        }

        if (!isTaskDispatcher || isLeafTaskDispatcher) {
            const edgeBetweenSubtasks = {
                id: `${sourceTaskName}=>${targetId}`,
                source: sourceTaskName,
                style: EDGE_STYLES,
                target: targetId,
                targetHandle: targetHandleId,
                type: 'workflow',
            };

            edges.push(edgeBetweenSubtasks);
        }
    });

    return edges;
}

/**
 * Creates all edges for the Loop task dispatcher
 */
export default function createLoopEdges(loopNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = loopNode.data as NodeDataType;

    const baseStructureEdges = createBaseLoopStructureEdges(loopNode.id);

    edges.push(...baseStructureEdges);

    if (!nodeData.parameters?.iteratee?.length) {
        const emptyLoopEdges = createEdgesForEmptyLoop(loopNode.id);

        edges.push(...emptyLoopEdges);
    } else {
        const loopChildTasks: Array<WorkflowTask> = nodeData.parameters.iteratee;

        const iterationEdges = createLoopSubtaskEdges(loopNode.id, loopChildTasks);

        edges.push(...iterationEdges);
    }

    return edges;
}
