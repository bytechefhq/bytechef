import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphPendingConnectionType, GraphTransitionType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {takeAutoPlacedGraphPositions} from './autoPlacedGraphPositions';
import {placeGraphMembers, readGraphMemberCanvasState} from './graphMemberPlacement';
import {addTransition} from './graphTransitionMutations';

interface ApplyGraphMemberInsertionPropsI {
    /** The graph task as it was before the member was appended. */
    previousGraphTask: WorkflowTask;
    /** The graph task with the member already appended to `parameters.nodes`. */
    updatedGraphTask: WorkflowTask;
    /** The pending connection raised for THIS graph, if any. */
    pendingConnection?: GraphPendingConnectionType;
    /** The current canvas nodes, read for the members' rendered positions and sizes. */
    nodes: Node[];
}

/**
 * Finishes a graph member insertion: the new task gets a concrete position — where it was dropped,
 * or a free spot beside its siblings — the graph's pending auto-placed positions are flushed
 * alongside it, and, when the insertion came from a transition released over empty frame space, the
 * transition that leads to it.
 *
 * Kept out of `updateTaskParameters` — which stays a pure `parameters` reshape, shared with the
 * field-change and node-data rebuild paths — so the canvas reads this needs do not pull the workflow
 * data store into `taskDispatcherConfig`. The layout utils already import that module, so a store
 * import there closes an initialisation cycle.
 */
export function applyGraphMemberInsertion({
    nodes,
    pendingConnection,
    previousGraphTask,
    updatedGraphTask,
}: ApplyGraphMemberInsertionPropsI): WorkflowTask {
    const graphId = updatedGraphTask.name;

    const {addedMemberName, members} = placeGraphMembers({
        autoPlacedPositions: takeAutoPlacedGraphPositions(graphId),
        canvasState: readGraphMemberCanvasState(graphId, nodes),
        dropPosition: pendingConnection?.dropPosition,
        previousMembers: (previousGraphTask.parameters?.nodes ?? []) as Array<WorkflowTask>,
        updatedMembers: (updatedGraphTask.parameters?.nodes ?? []) as Array<WorkflowTask>,
    });

    const transitions = (updatedGraphTask.parameters?.transitions ?? []) as Array<GraphTransitionType>;

    // An empty `from` is a plain add — a component dropped into the box raises a pending connection
    // purely to carry the drop position, with no edge to draw.
    const updatedTransitions =
        pendingConnection?.from && addedMemberName
            ? addTransition(transitions, pendingConnection.from, addedMemberName)
            : transitions;

    return {
        ...updatedGraphTask,
        parameters: {
            ...updatedGraphTask.parameters,
            nodes: members,
            transitions: updatedTransitions,
        },
    };
}
