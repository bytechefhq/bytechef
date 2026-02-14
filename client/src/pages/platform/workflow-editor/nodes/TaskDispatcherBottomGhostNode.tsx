import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

const TaskDispatcherBottomGhostNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const isHorizontal = layoutDirection === 'LR';

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
                className={twMerge(isHorizontal ? 'top-8' : 'left-8', styles.handle)}
                id={`${id}-left`}
                position={mapHandlePosition(Position.Left, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(isHorizontal ? 'bottom-8' : 'right-8', styles.handle)}
                id={`${id}-right`}
                position={mapHandlePosition(Position.Right, layoutDirection)}
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
