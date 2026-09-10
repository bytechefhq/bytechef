import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {memo, useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {twMerge} from 'tailwind-merge';

import {extractClusterElementIcons} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {mapHandlePosition} from '../utils/directionUtils';
import {CONFIGURED_CLUSTER_ROOT_HANDLE_OFFSET, REGULAR_NODE_HANDLE_OFFSET} from '../utils/postDagreConstraints';
import styles from './NodeTypes.module.css';

const MAX_VISIBLE_CLUSTER_ELEMENT_ICONS = 5;

const ReadOnlyNode = ({data}: {data: NodeDataType}) => {
    const layoutDirection = useLayoutDirectionStore((state) => state.layoutDirection);

    const clusterElementIcons = useMemo(() => {
        if (!data.clusterElements || Array.isArray(data.clusterElements)) {
            return [];
        }

        const icons = extractClusterElementIcons(data.clusterElements);

        if (!Array.isArray(icons)) {
            return [];
        }

        const uniqueIcons = icons.reduce((uniqueIconsList, iconItem) => {
            if (!uniqueIconsList.has(iconItem.icon)) {
                uniqueIconsList.set(iconItem.icon, iconItem);
            }

            return uniqueIconsList;
        }, new Map<string, {icon: string; label: string}>());

        return Array.from(uniqueIcons.values());
    }, [data.clusterElements]);

    const visibleClusterElementIcons = clusterElementIcons.slice(0, MAX_VISIBLE_CLUSTER_ELEMENT_ICONS);
    const remainingClusterElementIconCount = clusterElementIcons.length - visibleClusterElementIcons.length;
    const handleOffset = clusterElementIcons.length
        ? CONFIGURED_CLUSTER_ROOT_HANDLE_OFFSET
        : REGULAR_NODE_HANDLE_OFFSET;

    return (
        <div className="relative flex cursor-grab items-center justify-center">
            <div
                className={twMerge(
                    'flex items-center justify-center rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-4 text-primary shadow-sm',
                    clusterElementIcons.length ? 'h-auto min-h-18 flex-col' : 'size-18'
                )}
            >
                <span className="self-center text-content-neutral-primary [&_svg]:size-9">{data.icon}</span>

                {visibleClusterElementIcons.length > 0 && (
                    <ul className="mt-2 flex min-w-52 items-center justify-center">
                        {visibleClusterElementIcons.map((clusterElementIcon) => (
                            <li
                                className="mr-2 flex items-center justify-center rounded-full border bg-surface-neutral-primary p-1 [&_svg]:size-5"
                                key={clusterElementIcon.icon}
                                title={clusterElementIcon.label}
                            >
                                <InlineSVG
                                    className="size-9 flex-none text-content-neutral-primary"
                                    src={clusterElementIcon.icon}
                                />
                            </li>
                        ))}

                        {remainingClusterElementIconCount > 0 && (
                            <li className="text-xs text-content-neutral-secondary">
                                +{remainingClusterElementIconCount}
                            </li>
                        )}
                    </ul>
                )}
            </div>

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="font-semibold">{data.title || data.label}</span>

                {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

                <span className="text-sm text-content-neutral-secondary">{data.trigger ? 'trigger_1' : data.name}</span>
            </div>

            <Handle
                className={styles.handle}
                isConnectable={false}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                style={layoutDirection === 'TB' ? {left: `${handleOffset}px`} : undefined}
                type="target"
            />

            <Handle
                className={styles.handle}
                isConnectable={false}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                style={layoutDirection === 'TB' ? {left: `${handleOffset}px`} : undefined}
                type="source"
            />
        </div>
    );
};

export default memo(ReadOnlyNode);
