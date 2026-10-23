import {Handle, Position} from '@xyflow/react';

import {convertNameToCamelCase, getHandlePosition} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

interface ClusterSlotHandlesProps {
    clusterElementTypeNames: string[];
    layoutDirection: Parameters<typeof mapHandlePosition>[1];
    nodeWidth: number;
}

const ClusterSlotHandles = ({clusterElementTypeNames, layoutDirection, nodeWidth}: ClusterSlotHandlesProps) => (
    <>
        {clusterElementTypeNames.map((clusterElementTypeName, index) => (
            <Handle
                className={styles.handle}
                id={`${convertNameToCamelCase(clusterElementTypeName)}-handle`}
                isConnectable={false}
                key={clusterElementTypeName}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                style={{
                    left: `${getHandlePosition({
                        handlesCount: clusterElementTypeNames.length,
                        index,
                        nodeWidth,
                    })}px`,
                    transform: 'translateX(-50%)',
                }}
                type="source"
            />
        ))}
    </>
);

export default ClusterSlotHandles;
