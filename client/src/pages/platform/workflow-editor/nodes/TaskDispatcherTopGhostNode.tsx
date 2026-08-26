import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';
import TaskDispatcherGhostBarHalves from './TaskDispatcherGhostBarHalves';
import useTaskDispatcherGhostBarStatuses from './useTaskDispatcherGhostBarStatuses';

// ELK's iteration rings flip their content to the TOP edge in LR (see
// getRingContentSign in elkLayoutUtils): the rail leaves from the bar's
// bottom end and the body enters from its top end, so the side handles swap
// ends — otherwise the ring connectors would cut diagonally across the body.
// Dagre keeps the loop body BELOW the main axis in LR, so the flip applies
// only when ELK actually produced the current layout — flipped handles under
// dagre make every ring connector double back in box-shaped detours.
const LR_FLIPPED_RING_BAR_PATTERN = /-(loop|each|map)-(top|bottom)-ghost$/;

// A graph's members live free-form inside its frame, so nothing ever hangs off this bar the way a
// loop's body or a condition's branches do. Painting it would put a stray horizontal rule on the
// chain for no reader to interpret, so the graph's ghosts keep their handles and drop their ink.
const GRAPH_GHOST_BAR_PATTERN = /-graph-(top|bottom)-ghost$/;

const TaskDispatcherTopGhostNode = ({data, id}: {data?: unknown; id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const lastAppliedLayoutEngine = useLayoutEngineStore((state) => state.lastAppliedLayoutEngine);
    const isHorizontal = layoutDirection === 'LR';
    const isFlippedRingBar = isHorizontal && lastAppliedLayoutEngine === 'elk' && LR_FLIPPED_RING_BAR_PATTERN.test(id);

    const {leftStatus, rightStatus} = useTaskDispatcherGhostBarStatuses(id, data, false);

    const isGraphBar = GRAPH_GHOST_BAR_PATTERN.test(id);

    return (
        <div
            className={twMerge(
                'nodrag flex',
                isHorizontal ? 'h-[72px] w-0.5 flex-col' : 'h-0.5 w-[72px]',
                isGraphBar ? 'bg-transparent' : 'bg-stroke-neutral-tertiary'
            )}
            data-nodetype="taskDispatcherTopGhostNode"
            key={id}
        >
            {!isGraphBar && (
                <TaskDispatcherGhostBarHalves
                    isFlippedRingBar={isFlippedRingBar}
                    leftStatus={leftStatus}
                    rightStatus={rightStatus}
                />
            )}

            <Handle
                className={twMerge(styles.handle)}
                id={`${id}-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(isHorizontal ? (isFlippedRingBar ? 'top-8' : 'bottom-8') : 'right-8', styles.handle)}
                id={`${id}-right`}
                position={mapHandlePosition(isFlippedRingBar ? Position.Left : Position.Right, layoutDirection)}
                type="source"
            />

            <Handle
                className={twMerge(isHorizontal ? (isFlippedRingBar ? 'bottom-8' : 'top-8') : 'left-8', styles.handle)}
                id={`${id}-left`}
                position={mapHandlePosition(isFlippedRingBar ? Position.Right : Position.Left, layoutDirection)}
                type="source"
            />

            <Handle
                className={styles.handle}
                id={`${id}-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export default memo(TaskDispatcherTopGhostNode);
