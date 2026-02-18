import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

function createBaseEachStructureEdges(eachId: string): Edge[] {
    const topGhostId = `${eachId}-each-top-ghost`;
    const bottomGhostId = `${eachId}-each-bottom-ghost`;
    const leftGhostId = `${eachId}-taskDispatcher-left-ghost`;

    const edgeFromLoopToTopGhost = {
        id: `${eachId}=>${topGhostId}`,
        source: eachId,
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

function createEdgesForEmptyEach(eachId: string): Edge[] {
    const topGhostId = `${eachId}-each-top-ghost`;
    const bottomGhostId = `${eachId}-each-bottom-ghost`;
    const placeholderId = `${eachId}-each-placeholder-0`;

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

function createEdgeSubtaskEdges(eachId: string, eachChildTask: WorkflowTask): Edge[] {
    const topGhostId = `${eachId}-each-top-ghost`;
    const bottomGhostId = `${eachId}-each-bottom-ghost`;
    const childTaskId = eachChildTask.name;
    const childTaskComponentName = childTaskId.split('_')[0];

    const edgeFromTopGhostToChildTask = {
        id: `${topGhostId}=>${childTaskId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-right`,
        style: EDGE_STYLES,
        target: childTaskId,
        type: 'smoothstep',
    };

    // Check if the child task is itself a task dispatcher
    if (
        TASK_DISPATCHER_NAMES.includes(childTaskComponentName) &&
        !CHILDLESS_TASK_DISPATCHER_NAMES.includes(childTaskComponentName)
    ) {
        const nestedBottomGhostId = `${childTaskId}-${childTaskComponentName}-bottom-ghost`;

        const edgeFromNestedGhostToBottomGhost = {
            id: `${nestedBottomGhostId}=>${bottomGhostId}`,
            source: nestedBottomGhostId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-right`,
            type: 'smoothstep',
        };

        return [edgeFromTopGhostToChildTask, edgeFromNestedGhostToBottomGhost];
    } else {
        const edgeFromSubtaskToBottomGhost = {
            id: `${childTaskId}=>${bottomGhostId}`,
            source: childTaskId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-right`,
            type: 'smoothstep',
        };

        return [edgeFromTopGhostToChildTask, edgeFromSubtaskToBottomGhost];
    }
}

/**
 * Creates all edges for the Each task dispatcher
 */
export default function createEachEdges(eachNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = eachNode.data as NodeDataType;

    const baseStructureEdges = createBaseEachStructureEdges(eachNode.id);

    edges.push(...baseStructureEdges);

    if (!nodeData.parameters?.iteratee?.name) {
        const emptyLoopEdges = createEdgesForEmptyEach(eachNode.id);

        edges.push(...emptyLoopEdges);
    } else {
        const eachChildTask: WorkflowTask = nodeData.parameters.iteratee;

        const subtaskEdges = createEdgeSubtaskEdges(eachNode.id, eachChildTask);

        edges.push(...subtaskEdges);
    }

    return edges;
}
