import {LayoutDirectionType} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphPendingConnectionType, GraphTransitionType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {takeAutoPlacedGraphPositions} from './autoPlacedGraphPositions';
import {placeGraphMembers, readGraphMemberCanvasState} from './graphMemberPlacement';
import {addTransition} from './graphTransitionMutations';

interface ApplyGraphMemberInsertionProps {
    direction?: LayoutDirectionType;
    previousGraphTask: WorkflowTask;
    updatedGraphTask: WorkflowTask;
    pendingConnection?: GraphPendingConnectionType;
    nodes: Node[];
}

export function applyGraphMemberInsertion({
    direction,
    nodes,
    pendingConnection,
    previousGraphTask,
    updatedGraphTask,
}: ApplyGraphMemberInsertionProps): WorkflowTask {
    const graphId = updatedGraphTask.name;

    const autoPlacedPositions = takeAutoPlacedGraphPositions(graphId);
    const canvasState = readGraphMemberCanvasState(graphId, nodes);
    const previousMembers = (previousGraphTask.parameters?.nodes ?? []) as Array<WorkflowTask>;
    const updatedMembers = (updatedGraphTask.parameters?.nodes ?? []) as Array<WorkflowTask>;

    const {addedMemberName, members} = placeGraphMembers({
        autoPlacedPositions,
        canvasState,
        direction,
        dropPosition: pendingConnection?.dropPosition,
        previousMembers,
        updatedMembers,
    });

    const transitions = (updatedGraphTask.parameters?.transitions ?? []) as Array<GraphTransitionType>;

    const updatedTransitions =
        pendingConnection?.from && addedMemberName
            ? addTransition({from: pendingConnection.from, to: addedMemberName, transitions})
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
