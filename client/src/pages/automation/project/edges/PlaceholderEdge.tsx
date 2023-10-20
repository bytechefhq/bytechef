import React from 'react';
import {EdgeProps, getBezierPath} from 'reactflow';

import styles from './EdgeTypes.module.css';

export default function PlaceholderEdge({
    id,
    markerEnd,
    sourcePosition,
    sourceX,
    sourceY,
    style,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const [edgePath] = getBezierPath({
        sourcePosition,
        sourceX,
        sourceY,
        targetPosition,
        targetX,
        targetY,
    });

    return (
        <path
            id={id}
            style={style}
            className={styles.placeholderPath}
            d={edgePath}
            markerEnd={markerEnd}
        />
    );
}
