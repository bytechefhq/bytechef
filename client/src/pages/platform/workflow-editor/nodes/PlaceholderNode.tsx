import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {memo, useState} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const {nodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
        }))
    );

    const nodeIndex = nodes.findIndex((node) => node.id === id);
    const isClusterElement = !!data.clusterElementType;
    const rootClusterElementId = id.split('-')[0];
    const effectiveDirection = isClusterElement ? 'TB' : layoutDirection;

    return (
        <WorkflowNodesPopoverMenu
            clusterElementType={data.clusterElementType}
            hideActionComponents={!!data.clusterElementType}
            hideClusterElementComponents={!data.clusterElementType}
            hideTaskDispatchers={!!data.clusterElementType}
            hideTriggerComponents
            key={`${id}-${nodeIndex}`}
            multipleClusterElementsNode={data.multipleClusterElementsNode}
            nodeIndex={nodeIndex}
            sourceNodeId={data.clusterElementType ? rootClusterElementId : id}
        >
            <div
                className={twMerge(
                    'relative mx-[22px] flex cursor-pointer items-center justify-center rounded-md text-lg text-gray-500 shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                    isDropzoneActive
                        ? 'absolute ml-2 size-16 scale-150 cursor-pointer bg-blue-100'
                        : 'size-7 bg-gray-300',
                    isClusterElement && 'mx-0 size-6'
                )}
                onDragEnter={() => setDropzoneActive(true)}
                onDragLeave={() => setDropzoneActive(false)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => setDropzoneActive(false)}
                title="Click to add a node"
            >
                {data.label}

                <Handle
                    className={styles.handle}
                    position={mapHandlePosition(Position.Top, effectiveDirection)}
                    type="target"
                />

                <Handle
                    className={styles.handle}
                    position={mapHandlePosition(Position.Bottom, effectiveDirection)}
                    type="source"
                />
            </div>
        </WorkflowNodesPopoverMenu>
    );
};

export default memo(PlaceholderNode);
