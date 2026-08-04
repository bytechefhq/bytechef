import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

// ELK's iteration rings flip their content to the TOP edge in LR (see
// getRingContentSign in elkLayoutUtils): the rail returns into the bar's
// bottom end and the body exits into its top end, so the side handles swap
// ends — otherwise the ring connectors would cut diagonally across the body.
// Dagre keeps the loop body BELOW the main axis in LR, so the flip applies
// only when ELK actually produced the current layout — flipped handles under
// dagre make every ring connector double back in box-shaped detours.
const LR_FLIPPED_RING_BAR_PATTERN = /-(loop|each|map)-(top|bottom)-ghost$/;

const TaskDispatcherBottomGhostNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const lastAppliedLayoutEngine = useLayoutEngineStore((state) => state.lastAppliedLayoutEngine);
    const isHorizontal = layoutDirection === 'LR';
    const isFlippedRingBar = isHorizontal && lastAppliedLayoutEngine === 'elk' && LR_FLIPPED_RING_BAR_PATTERN.test(id);

    return (
        <div
            className={twMerge(
                'nodrag',
                isHorizontal ? 'h-[72px] w-0.5' : 'h-0.5 w-[72px]',
                'bg-stroke-neutral-tertiary'
            )}
            data-nodetype="taskDispatcherBottomGhostNode"
            key={id}
        >
            <Handle
                className={twMerge(styles.handle)}
                id={`${id}-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(isHorizontal ? (isFlippedRingBar ? 'bottom-8' : 'top-8') : 'left-8', styles.handle)}
                id={`${id}-left`}
                position={mapHandlePosition(isFlippedRingBar ? Position.Right : Position.Left, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(isHorizontal ? (isFlippedRingBar ? 'top-8' : 'bottom-8') : 'right-8', styles.handle)}
                id={`${id}-right`}
                position={mapHandlePosition(isFlippedRingBar ? Position.Left : Position.Right, layoutDirection)}
                type="target"
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

export default memo(TaskDispatcherBottomGhostNode);
