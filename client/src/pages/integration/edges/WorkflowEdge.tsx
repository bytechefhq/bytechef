import React from 'react';
import {EdgeProps, getBezierPath} from 'reactflow';

import useEdgeClickHook from '../hooks/useEdgeClick.hook';
import styles from './EdgeTypes.module.css';
import PopoverMenu from '../components/PopoverMenu';

export default function WorkflowEdge({
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    style,
    markerEnd,
}: EdgeProps) {
    // see the hook for implementation details
    // onClick adds a node in between the nodes that are connected by this edge
    const onClick = useEdgeClickHook(id);

    const [edgePath, edgeCenterX, edgeCenterY] = getBezierPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX,
        targetY,
        targetPosition,
    });

    return (
        <>
            <path
                id={id}
                style={style}
                className={styles.edgePath}
                d={edgePath}
                markerEnd={markerEnd}
            />

            <PopoverMenu>
                <g transform={`translate(${edgeCenterX}, ${edgeCenterY})`}>
                    <rect
                        onClick={onClick}
                        x={-10}
                        y={-10}
                        width={20}
                        ry={4}
                        rx={4}
                        height={20}
                        className={styles.edgeButton}
                    />

                    <text className={styles.edgeButtonText} y={5} x={-5}>
                        +
                    </text>
                </g>
            </PopoverMenu>
        </>
    );
}
