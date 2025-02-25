import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import styles from './NodeTypes.module.css';

const TaskDispatcherBottomGhostNode = ({id}: {id: string}) => {
    return (
        <div className="h-0.5 w-[74px] bg-gray-300" data-nodetype="taskDispatcherBottomGhostNode" key={id}>
            <Handle
                className={twMerge('left-8', styles.handle)}
                id={`${id}-bottom-ghost-left`}
                position={Position.Left}
                type="target"
            />

            <Handle
                className={twMerge('right-8', styles.handle)}
                id={`${id}-bottom-ghost-right`}
                position={Position.Right}
                type="target"
            />

            <Handle
                className={twMerge(styles.handle)}
                id={`${id}-bottom-ghost-bottom`}
                position={Position.Bottom}
                type="source"
            />
        </div>
    );
};

export default memo(TaskDispatcherBottomGhostNode);
