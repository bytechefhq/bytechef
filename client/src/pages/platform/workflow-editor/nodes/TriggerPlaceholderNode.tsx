import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

const TriggerPlaceholderNode = ({data, id}: {data: NodeDataType; id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    return (
        <WorkflowNodesPopoverMenu
            hideActionComponents
            hideClusterElementComponents
            hideTaskDispatchers
            sourceNodeId={id}
        >
            <div
                className={twMerge(
                    'nodrag relative mx-[22px] flex size-12 cursor-pointer items-center justify-center rounded-md border-2 border-dashed border-stroke-neutral-tertiary bg-surface-neutral-primary text-2xl text-content-neutral-secondary shadow-none hover:scale-110 hover:border-stroke-brand-secondary-hover hover:text-content-neutral-primary'
                )}
                title="Click to add a trigger"
            >
                {data.label}

                <Handle
                    className={twMerge(styles.handle, 'invisible')}
                    position={mapHandlePosition(Position.Bottom, layoutDirection)}
                    type="source"
                />
            </div>
        </WorkflowNodesPopoverMenu>
    );
};

export default memo(TriggerPlaceholderNode);
