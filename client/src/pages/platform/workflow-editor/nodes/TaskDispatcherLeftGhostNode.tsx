import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

const TaskDispatcherLeftGhostNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const isHorizontal = layoutDirection === 'LR';

    return (
        <div
            className={twMerge('nodrag', isHorizontal ? 'h-0.5 w-4' : 'h-4 w-0.5', 'bg-stroke-neutral-tertiary')}
            data-nodetype="taskDispatcherLeftGhostNode"
            key={id}
        >
            <Handle
                className={twMerge(isHorizontal ? 'left-8' : 'top-8', styles.handle)}
                id={`${id}-left-ghost-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(styles.handle)}
                id={`${id}-left-ghost-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export default memo(TaskDispatcherLeftGhostNode);
