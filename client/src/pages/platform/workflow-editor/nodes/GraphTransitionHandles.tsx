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
