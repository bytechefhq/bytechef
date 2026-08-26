import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {GRAPH_START_SOURCE_HANDLE_SUFFIX} from '../utils/graph/graphHandleSuffixes';
import styles from './NodeTypes.module.css';

/**
 * The non-deletable Start pill at a graph frame's content origin. It carries no task of its own —
 * the single `graphStart` edge leaving it is what marks which member the graph enters at
 * (`parameters.startNode`).
 *
 * Its one handle sits on the main axis, the same side a member's transition source does, so the
 * start edge leaves the pill the way every transition leaves a member.
 */
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
