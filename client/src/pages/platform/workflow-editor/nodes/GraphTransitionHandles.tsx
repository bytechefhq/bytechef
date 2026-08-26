import {LayoutDirectionType} from '@/shared/constants';
import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';

import {mapHandlePosition} from '../utils/directionUtils';
import {
    GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX,
    GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX,
} from '../utils/graph/graphHandleSuffixes';
import styles from './NodeTypes.module.css';

interface GraphTransitionHandlesProps {
    boxWidth?: number;
    connectable: boolean;
    direction: LayoutDirectionType;
    nodeId: string;
}

/**
 * The transition endpoints a task carries while it is a member of a `graph/v1` frame. Every node
 * component that can render a member (`WorkflowNode`, `AiAgentNode`, and `ReadOnlyNode` in the
 * read-only and execution views) renders these, and only when `data.graphData` says the node really
 * is one — they are the canvas's only connectable handles, so offering them on an ordinary node
 * would let a user draw a transition between two tasks that belong to no graph at all.
 *
 * They sit on the MAIN axis of the layout direction (TB: target top, source bottom; LR: target
 * left, source right), so a transition enters and leaves a member the same way the surrounding
 * chain reads — including a transition that doubles back, which leaves and re-enters through the
 * same two handles as every other one. What differs for those is only the ROUTE between them: see
 * `computeGraphLoopPath`, which carries them out to a lane beside the member block rather than
 * back along the lane they came down.
 *
 * `boxWidth` is the painted box's width, which in TB is NOT the node element's: the element also
 * spans the label beside the box, so React Flow's own centring would drift the endpoints out over
 * the label. Callers pass the box width and the handles are centred on it, matching the offset
 * their own chain handles already use. In LR the label is stacked under the box, so the element IS
 * the box and React Flow's centring is already right.
 *
 * Every target handle sets `isConnectableStart={false}`. React Flow defaults that prop to true and
 * consults it ALONE when deciding whether a pointer-down may begin a drag — `isConnectable` does
 * not gate it — so without this a user could drag backwards out of a target handle and the
 * transition would be authored pointing the opposite way from the one they drew.
 */
const GraphTransitionHandles = ({boxWidth, connectable, direction, nodeId}: GraphTransitionHandlesProps) => {
    const boxCenterStyle = direction === 'TB' && boxWidth ? {left: `${boxWidth / 2}px`} : undefined;

    return (
        <>
            <Handle
                className={connectable ? styles.handleConnectable : styles.handle}
                id={`${nodeId}${GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX}`}
                isConnectable={connectable}
                isConnectableStart={false}
                position={mapHandlePosition(Position.Top, direction)}
                style={boxCenterStyle}
                type="target"
            />

            <Handle
                className={connectable ? styles.handleConnectable : styles.handle}
                id={`${nodeId}${GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX}`}
                isConnectable={connectable}
                position={mapHandlePosition(Position.Bottom, direction)}
                style={boxCenterStyle}
                type="source"
            />

            {/* A dynamic transition's `to` is an expression resolved at run time, so it names no
                member: its edge returns to its own source and ends here, beside the handle it left.
                An anchor, never a drop target. */}
            <Handle
                className={styles.handle}
                id={`${nodeId}${GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX}`}
                isConnectable={false}
                isConnectableStart={false}
                position={mapHandlePosition(Position.Bottom, direction)}
                style={boxCenterStyle}
                type="target"
            />
        </>
    );
};

export default memo(GraphTransitionHandles);
