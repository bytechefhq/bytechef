// Ported from client/src/pages/platform/workflow-editor/nodes/TaskDispatcherLeftGhostNode.tsx.
// Adaptations: store-free (pinned TB direction), `invisible` utility instead of the
// Tailwind-v4 NodeTypes.module.css handle class.

import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import {mapHandlePosition} from '../layout/directionUtils';
import {useLayoutDirection} from '../useLayoutDirection';

const TaskDispatcherLeftGhostNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirection();

    const isHorizontal = layoutDirection === 'LR';

    return (
        <div
            className={twMerge('nodrag', isHorizontal ? 'h-0.5 w-4' : 'h-4 w-0.5', 'bg-stroke-neutral-tertiary')}
            data-nodetype="taskDispatcherLeftGhostNode"
            key={id}
        >
            <Handle
                className={twMerge(isHorizontal ? 'left-8' : 'top-8', 'invisible')}
                id={`${id}-left-ghost-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className="invisible"
                id={`${id}-left-ghost-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export default memo(TaskDispatcherLeftGhostNode);
