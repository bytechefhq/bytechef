import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import styles from './NodeTypes.module.css';

const TaskDispatcherLeftGhostNode = ({id}: {id: string}) => (
    <div className="h-4 w-0.5 bg-gray-300" data-nodetype="taskDispatcherLeftGhostNode" key={id}>
        <Handle
            className={twMerge('top-8', styles.handle)}
            id={`${id}-left-ghost-top`}
            position={Position.Top}
            type="target"
        />

        <Handle
            className={twMerge(styles.handle)}
            id={`${id}-left-ghost-bottom`}
            position={Position.Bottom}
            type="source"
        />
    </div>
);

export default memo(TaskDispatcherLeftGhostNode);
