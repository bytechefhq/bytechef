import {CHILDLESS_TASK_DISPATCHER_NAMES, EDGE_STYLES, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

/**
 * Creates the base map structure edges (map -> top ghost -> left ghost -> bottom ghost)
 */
function createBaseMapStructureEdges(mapId: string): Edge[] {
    const topGhostId = `${mapId}-map-top-ghost`;
    const bottomGhostId = `${mapId}-map-bottom-ghost`;
    const leftGhostId = `${mapId}-taskDispatcher-left-ghost`;

    const edgeFromMapToTopGhost = {
        id: `${mapId}=>${topGhostId}`,
        source: mapId,
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

    return [edgeFromMapToTopGhost, edgeFromTopGhostToLeftGhost, edgeFromLeftGhostToBottomGhost];
}

/**
 * Creates edges for empty map (with placeholder)
 */
function createEdgesForEmptyMap(mapId: string): Edge[] {
    const topGhostId = `${mapId}-map-top-ghost`;
    const bottomGhostId = `${mapId}-map-bottom-ghost`;
    const placeholderId = `${mapId}-map-placeholder-0`;

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
 * Creates edges between map tasks
 */
function createMapSubtaskEdges(mapId: string, mapChildTasks: Array<WorkflowTask>): Edge[] {
    const edges: Edge[] = [];

    const topGhostId = `${mapId}-map-top-ghost`;
    const bottomGhostId = `${mapId}-map-bottom-ghost`;

    if (mapChildTasks.length === 0) {
        return [];
    }

    const edgeFromTopGhostToFirstMapChildTask = {
        id: `${topGhostId}=>${mapChildTasks[0].name}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-right`,
        style: EDGE_STYLES,
        target: mapChildTasks[0].name,
        type: 'workflow',
    };

    edges.push(edgeFromTopGhostToFirstMapChildTask);

    mapChildTasks.forEach((task, index) => {
        const sourceTaskName = task.name;
        const sourceTaskComponentName = task.name.split('_')[0];

        const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName);
        const isLeafTaskDispatcher = CHILDLESS_TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName);
        const isLastTask = index === mapChildTasks.length - 1;

        let targetId;
        let targetHandleId;

        if (isLastTask) {
            targetId = bottomGhostId;
            targetHandleId = `${bottomGhostId}-right`;
        } else {
            targetId = mapChildTasks[index + 1].name;
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
 * Creates all edges for the Map task dispatcher
 */
export default function createMapEdges(mapNode: Node): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = mapNode.data as NodeDataType;

    const baseStructureEdges = createBaseMapStructureEdges(mapNode.id);

    edges.push(...baseStructureEdges);

    if (!nodeData.parameters?.iteratee?.length) {
        const emptyMapEdges = createEdgesForEmptyMap(mapNode.id);

        edges.push(...emptyMapEdges);
    } else {
        const mapChildTasks: Array<WorkflowTask> = nodeData.parameters.iteratee;

        const iterationEdges = createMapSubtaskEdges(mapNode.id, mapChildTasks);

        edges.push(...iterationEdges);
    }

    return edges;
}
