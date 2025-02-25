import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import styles from './NodeTypes.module.css';

const LoopLeftGhostNode = ({id}: {id: string}) => {
    return (
        <div className="h-4 w-0.5 bg-gray-300" data-nodetype="loopLeftGhostNode" key={id}>
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
};

export default memo(LoopLeftGhostNode);
