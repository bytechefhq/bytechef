import {memo, useState} from 'react';
import {Handle, NodeProps, Position} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: NodeProps) => {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

    return (
        <WorkflowNodesPopoverMenu conditionId={data.conditionId} hideTriggerComponents sourceNodeId={id}>
            <div
                className={twMerge(
                    'mx-placeholder-node-position flex cursor-pointer items-center justify-center rounded-md text-lg text-gray-500 shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                    isDropzoneActive ? 'mx-1.5 size-16 scale-150 cursor-pointer bg-blue-100' : 'size-7 bg-gray-300'
                )}
                onDragEnter={() => setDropzoneActive(true)}
                onDragLeave={() => setDropzoneActive(false)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => setDropzoneActive(false)}
                title="Click to add a node"
            >
                {data.label}

                <Handle className={styles.handle} isConnectable={false} position={Position.Top} type="target" />

                <Handle className={styles.handle} isConnectable={false} position={Position.Bottom} type="source" />
            </div>
        </WorkflowNodesPopoverMenu>
    );
};

export default memo(PlaceholderNode);
