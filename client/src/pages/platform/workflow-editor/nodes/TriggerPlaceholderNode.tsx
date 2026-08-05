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
                {/* Connector back to the trigger row, so the slot reads as the next trigger rather
                    than as a stray box. Drawn here rather than as an edge because this node is
                    positioned by hand after layout (positionTriggerPlaceholder), so no layout edge
                    reaches it.

                    Spans the single gap positionTriggerPlaceholder leaves past what the trigger
                    occupies — plus this box's own margin in TB, where the margin is horizontal. */}

                <div
                    aria-hidden
                    className={twMerge(
                        'pointer-events-none absolute border-dashed border-stroke-neutral-tertiary',
                        layoutDirection === 'LR'
                            ? 'bottom-full left-1/2 h-10 -translate-x-1/2 border-l-2'
                            : 'top-1/2 right-full w-12 -translate-y-1/2 border-t-2'
                    )}
                />

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
