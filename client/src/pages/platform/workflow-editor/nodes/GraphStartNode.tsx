import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {GRAPH_START_SOURCE_HANDLE_SUFFIX} from '../utils/graph/graphHandleSuffixes';
import styles from './NodeTypes.module.css';

const GraphStartNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    return (
        <div
            className="nodrag flex size-full items-center justify-center rounded-full border border-stroke-neutral-secondary bg-surface-neutral-primary text-xs font-semibold text-content-neutral-secondary"
            data-nodetype="graphStart"
        >
            Start
            <Handle
                className={styles.handle}
                id={`${id}${GRAPH_START_SOURCE_HANDLE_SUFFIX}`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export default memo(GraphStartNode);
