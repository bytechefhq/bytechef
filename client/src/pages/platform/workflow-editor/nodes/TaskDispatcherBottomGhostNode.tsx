import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import styles from './NodeTypes.module.css';

const TaskDispatcherBottomGhostNode = ({id}: {id: string}) => (
    <div className={twMerge('h-0.5 w-[72px] bg-gray-300')} data-nodetype="taskDispatcherBottomGhostNode" key={id}>
        <Handle className={twMerge(styles.handle)} id={`${id}-top`} position={Position.Top} type="target" />

        <Handle className={twMerge('left-8', styles.handle)} id={`${id}-left`} position={Position.Left} type="target" />

        <Handle
            className={twMerge('right-8', styles.handle)}
            id={`${id}-right`}
            position={Position.Right}
            type="target"
        />

        <Handle className={styles.handle} id={`${id}-bottom`} position={Position.Bottom} type="source" />
    </div>
);

export default memo(TaskDispatcherBottomGhostNode);
