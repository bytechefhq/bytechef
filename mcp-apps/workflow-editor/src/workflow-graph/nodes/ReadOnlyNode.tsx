import {Handle, Position} from '@xyflow/react';
import {PlayIcon} from 'lucide-react';
import {memo} from 'react';

import {ComponentImage} from '../../components/ComponentImage';
import {buildIconUrl} from '../../iconUrl';
import {mapHandlePosition} from '../layout/directionUtils';
import {ReadOnlyNodeDataType} from '../types';
import {useLayoutDirection} from '../useLayoutDirection';

function ReadOnlyNode({data}: {data: ReadOnlyNodeDataType}) {
    const layoutDirection = useLayoutDirection();

    const isHorizontal = layoutDirection === 'LR';
    const nodeName = data.trigger ? 'trigger_1' : data.name;

    return (
        <div className={isHorizontal ? 'relative' : 'relative flex items-center justify-center'}>
            <div className="flex size-[72px] shrink-0 items-center justify-center rounded-md border-2 border-stroke-neutral-tertiary bg-surface-neutral-primary p-4 shadow-sm">
                {data.componentName === 'manual' ? (
                    <PlayIcon className="size-9 text-content-neutral-secondary" />
                ) : (
                    <ComponentImage
                        alt={`${data.title || data.label} logo`}
                        size={36}
                        src={buildIconUrl(data.componentName)}
                    />
                )}
            </div>

            <div
                className={
                    isHorizontal
                        ? 'absolute left-1/2 top-[80px] flex w-[150px] -translate-x-1/2 flex-col items-center text-center'
                        : 'ml-2 flex w-full min-w-max flex-col items-start'
                }
            >
                <span className={isHorizontal ? 'w-full truncate font-semibold' : 'font-semibold'}>
                    {data.title || data.label}
                </span>

                {data.operationName && (
                    <pre className={isHorizontal ? 'w-full truncate text-sm' : 'text-sm'}>{data.operationName}</pre>
                )}

                <span
                    className={
                        isHorizontal
                            ? 'w-full truncate text-sm text-content-neutral-secondary'
                            : 'text-sm text-content-neutral-secondary'
                    }
                >
                    {nodeName}
                </span>
            </div>

            <Handle
                className="invisible"
                isConnectable={false}
                position={mapHandlePosition(Position.Top, layoutDirection)}
                style={isHorizontal ? undefined : {left: '36px'}}
                type="target"
            />

            <Handle
                className="invisible"
                isConnectable={false}
                position={mapHandlePosition(Position.Bottom, layoutDirection)}
                style={isHorizontal ? undefined : {left: '36px'}}
                type="source"
            />
        </div>
    );
}

export default memo(ReadOnlyNode);
