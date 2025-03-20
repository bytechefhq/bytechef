import {EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates the base loop structure edges (loop -> top ghost -> left ghost -> bottom ghost)
 */
function createBaseLoopStructureEdges(loopNode: Node): Edge[] {
    const nodeId = loopNode.id;

    const edgeFromLoopToTopGhost = {
        id: `${nodeId}=>${nodeId}-loop-top-ghost`,
        source: nodeId,
        style: EDGE_STYLES,
        target: `${nodeId}-loop-top-ghost`,
        type: 'smoothstep',
    };

    const edgeFromTopGhostToLeftGhost = {
        id: `${nodeId}-loop-top-ghost=>${nodeId}-loop-left-ghost`,
        source: `${nodeId}-loop-top-ghost`,
        sourceHandle: `${nodeId}-loop-top-ghost-left`,
        style: EDGE_STYLES,
        target: `${nodeId}-loop-left-ghost`,
        type: 'smoothstep',
    };

    const edgeFromLeftGhostToBottomGhost = {
        id: `${nodeId}-loop-left-ghost=>${nodeId}-loop-bottom-ghost`,
        source: `${nodeId}-loop-left-ghost`,
        style: EDGE_STYLES,
        target: `${nodeId}-loop-bottom-ghost`,
        targetHandle: `${nodeId}-loop-bottom-ghost-left`,
        type: 'smoothstep',
    };

    return [edgeFromLoopToTopGhost, edgeFromTopGhostToLeftGhost, edgeFromLeftGhostToBottomGhost];
}

/**
 * Creates edges for empty loop (with placeholder)
 */
function createEdgesForEmptyLoop(loopNode: Node): Edge[] {
    const nodeId = loopNode.id;

    const edgeFromTopGhostToPlaceholder = {
        id: `${nodeId}-loop-top-ghost=>${nodeId}-loop-placeholder-0`,
        source: `${nodeId}-loop-top-ghost`,
        sourceHandle: `${nodeId}-loop-top-ghost-right`,
        style: EDGE_STYLES,
        target: `${nodeId}-loop-placeholder-0`,
        type: 'smoothstep',
    };

    const edgeFromPlaceholderToBottomGhost = {
        id: `${nodeId}-loop-placeholder-0=>${nodeId}-loop-bottom-ghost`,
        source: `${nodeId}-loop-placeholder-0`,
        style: EDGE_STYLES,
        target: `${nodeId}-loop-bottom-ghost`,
        targetHandle: `${nodeId}-loop-bottom-ghost-right`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToPlaceholder, edgeFromPlaceholderToBottomGhost];
}

/**
 * Creates edges between loop tasks
 */
function createLoopSubtaskEdges(loopNode: Node, loopChildTasks: Array<WorkflowTask>): Edge[] {
    const edges: Edge[] = [];
    const nodeId = loopNode.id;

    if (loopChildTasks.length === 0) {
        return [];
    }

    const edgeFromTopGhostToFirstLoopChildTask = {
        id: `${nodeId}-loop-top-ghost=>${loopChildTasks[0].name}`,
        source: `${nodeId}-loop-top-ghost`,
        sourceHandle: `${nodeId}-loop-top-ghost-right`,
        style: EDGE_STYLES,
        target: loopChildTasks[0].name,
        type: 'workflow',
    };

    edges.push(edgeFromTopGhostToFirstLoopChildTask);

    loopChildTasks.forEach((task, index) => {
        const sourceTaskName = task.name;
        const sourceTaskComponentName = task.name.split('_')[0];
        const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName);
        const isLastTask = index === loopChildTasks.length - 1;

        let targetId;
        let targetHandleId;

        if (isLastTask) {
            targetId = `${nodeId}-loop-bottom-ghost`;
            targetHandleId = `${nodeId}-loop-bottom-ghost-right`;
        } else {
            targetId = loopChildTasks[index + 1].name;
            targetHandleId = undefined;
        }

        if (isTaskDispatcher) {
            let bottomGhostId;

            if (sourceTaskComponentName === 'condition') {
                bottomGhostId = `${sourceTaskName}-condition-bottom-ghost`;
            } else if (sourceTaskComponentName === 'loop') {
                bottomGhostId = `${sourceTaskName}-loop-bottom-ghost`;
            }

            if (bottomGhostId) {
                const edge = {
                    id: `${bottomGhostId}=>${targetId}`,
                    source: bottomGhostId,
                    sourceHandle: isLastTask ? undefined : `${bottomGhostId}-bottom`,
                    style: EDGE_STYLES,
                    target: targetId,
                    targetHandle: targetHandleId,
                    type: 'workflow',
                };

                edges.push(edge);
            }
        } else {
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
 * Creates all edges for a loop node and its iteratees
 */
export default function createLoopEdges(loopNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = loopNode.data as NodeDataType;

    const baseStructureEdges = createBaseLoopStructureEdges(loopNode);

    edges.push(...baseStructureEdges);

    if (!nodeData.parameters?.iteratee?.length) {
        const emptyLoopEdges = createEdgesForEmptyLoop(loopNode);

        edges.push(...emptyLoopEdges);
    } else {
        const loopChildTasks: Array<WorkflowTask> = nodeData.parameters.iteratee;

        const iterationEdges = createLoopSubtaskEdges(loopNode, loopChildTasks);

        edges.push(...iterationEdges);
    }

    return edges;
}
