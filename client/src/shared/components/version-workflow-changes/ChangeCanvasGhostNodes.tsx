import styles from '@/pages/platform/workflow-editor/nodes/NodeTypes.module.css';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {mapHandlePosition} from '@/pages/platform/workflow-editor/utils/directionUtils';
import {Handle, Position} from '@xyflow/react';
import {twMerge} from 'tailwind-merge';

interface GhostBarPropsI {
    id: string;
    variant: 'bottom' | 'top';
}

const GhostBar = ({id, variant}: GhostBarPropsI) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const isHorizontal = layoutDirection === 'LR';
    const isTop = variant === 'top';

    const sideHandleType = isTop ? 'source' : 'target';

    return (
        <div
            className={twMerge(
                'nodrag flex bg-stroke-neutral-tertiary',
                isHorizontal ? 'h-[72px] w-0.5 flex-col' : 'h-0.5 w-[72px]'
            )}
            data-nodetype={isTop ? 'taskDispatcherTopGhostNode' : 'taskDispatcherBottomGhostNode'}
        >
            <Handle
                className={styles.handle}
                id={`${id}-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={twMerge(
                    isHorizontal ? (isTop ? 'top-8' : 'bottom-8') : isTop ? 'left-8' : 'right-8',
                    styles.handle
                )}
                id={isTop ? `${id}-left` : `${id}-right`}
                position={mapHandlePosition(isTop ? Position.Left : Position.Right, layoutDirection)}
                type={sideHandleType}
            />

            <Handle
                className={twMerge(
                    isHorizontal ? (isTop ? 'bottom-8' : 'top-8') : isTop ? 'right-8' : 'left-8',
                    styles.handle
                )}
                id={isTop ? `${id}-right` : `${id}-left`}
                position={mapHandlePosition(isTop ? Position.Right : Position.Left, layoutDirection)}
                type={sideHandleType}
            />

            <Handle
                className={styles.handle}
                id={`${id}-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};

export const ChangeCanvasTopGhostNode = ({id}: {id: string}) => <GhostBar id={id} variant="top" />;

export const ChangeCanvasBottomGhostNode = ({id}: {id: string}) => <GhostBar id={id} variant="bottom" />;

export const ChangeCanvasLeftGhostNode = ({id}: {id: string}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const isHorizontal = layoutDirection === 'LR';

    return (
        <div
            className={twMerge('nodrag bg-stroke-neutral-tertiary', isHorizontal ? 'h-0.5 w-4' : 'h-4 w-0.5')}
            data-nodetype="taskDispatcherLeftGhostNode"
        >
            <Handle
                className={twMerge(isHorizontal ? 'left-8' : 'top-8', styles.handle)}
                id={`${id}-left-ghost-top`}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                type="target"
            />

            <Handle
                className={styles.handle}
                id={`${id}-left-ghost-bottom`}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                type="source"
            />
        </div>
    );
};
