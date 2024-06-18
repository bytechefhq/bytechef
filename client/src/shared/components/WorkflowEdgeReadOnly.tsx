import {EdgeProps, getBezierPath} from 'reactflow';

import styles from './WorkflowEdgeReadOnly.module.css';

export default function WorkflowEdgeReadOnly({
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

    return <path className={styles.placeholderPath} d={edgePath} id={id} markerEnd={markerEnd} style={style} />;
}
