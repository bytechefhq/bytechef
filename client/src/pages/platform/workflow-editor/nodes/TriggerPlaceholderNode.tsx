import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {PlusIcon} from 'lucide-react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import WorkflowNodesPopoverMenu from '../components/WorkflowNodesPopoverMenu';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import styles from './NodeTypes.module.css';

const TriggerPlaceholderNode = ({id}: {data: NodeDataType; id: string}) => {
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
                    'nodrag relative mx-2 flex size-12 cursor-pointer items-center justify-center rounded-md border-2 border-dashed border-stroke-neutral-tertiary bg-surface-neutral-primary text-content-neutral-secondary shadow-none hover:scale-105 hover:border-stroke-brand-secondary-hover hover:text-content-neutral-primary'
                )}
                title="Click to add a trigger"
            >
                {/* A glyph "+" sits off true center (font metrics) and cannot grow past its line box; the icon
                    centers exactly in the flex box and scales freely. */}

                <PlusIcon className="size-6" />

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
