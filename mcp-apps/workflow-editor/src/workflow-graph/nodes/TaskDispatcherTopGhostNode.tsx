// Ported from client/src/pages/platform/workflow-editor/nodes/TaskDispatcherTopGhostNode.tsx.
// Adaptations: store-free (pinned TB direction), `invisible` utility instead of the
// Tailwind-v4 NodeTypes.module.css handle class.

import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import {mapHandlePosition} from '../layout/directionUtils';
import {useLayoutDirection} from '../useLayoutDirection';

const TaskDispatcherTopGhostNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirection();

    const isHorizontal = layoutDirection === 'LR';

    return (
        <div
            className={twMerge(
                'nodrag',
                isHorizontal ? 'h-[72px] w-0.5' : 'h-0.5 w-[72px]',
                'bg-stroke-neutral-tertiary'
            )}
            data-nodetype="taskDispatcherTopGhostNode"
            key={id}
        >
            <Handle
                className="invisible"
                id={`${id}-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(isHorizontal ? 'bottom-8' : 'right-8', 'invisible')}
                id={`${id}-right`}
                position={mapHandlePosition(Position.Right, layoutDirection)}
                type="source"
            />

            <Handle
                className={twMerge(isHorizontal ? 'top-8' : 'left-8', 'invisible')}
                id={`${id}-left`}
                position={mapHandlePosition(Position.Left, layoutDirection)}
                type="source"
            />

            <Handle
                className="invisible"
                id={`${id}-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export default memo(TaskDispatcherTopGhostNode);
