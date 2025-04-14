import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {memo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const nodeIndex = nodes.findIndex((node) => node.id === id);

    return (
        <WorkflowNodesPopoverMenu
            hideTriggerComponents
            key={`${id}-${nodeIndex}`}
            nodeIndex={nodeIndex}
            sourceNodeId={id}
        >
            <div
                className={twMerge(
                    'mx-placeholder-node-position flex cursor-pointer items-center justify-center rounded-md text-lg text-gray-500 shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                    isDropzoneActive
                        ? 'absolute ml-2 size-16 scale-150 cursor-pointer bg-blue-100'
                        : 'size-7 bg-gray-300'
                )}
                onDragEnter={() => setDropzoneActive(true)}
                onDragLeave={() => setDropzoneActive(false)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => setDropzoneActive(false)}
                title="Click to add a node"
            >
                {data.label}

                <Handle className={styles.handle} position={Position.Top} type="target" />

                <Handle className={styles.handle} position={Position.Bottom} type="source" />
            </div>
        </WorkflowNodesPopoverMenu>
    );
};

export default memo(PlaceholderNode);
