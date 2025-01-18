import {Button} from '@/components/ui/button';
import {NodeDataType} from '@/shared/types';
import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

import styles from '../../styles/WorkflowNode.module.css';

const ReadOnlyNode = ({data}: {data: NodeDataType}) => (
    <div className="relative flex cursor-grab items-center justify-center">
        <Button
            className={twMerge(
                'h-18 w-18 cursor-grab rounded-md border-2 border-gray-300 bg-white p-4 shadow hover:bg-white [&_svg]:size-9'
            )}
        >
            {data.icon}
        </Button>

        <div className="ml-2 flex w-full min-w-max flex-col items-start">
            <span className="font-semibold">{data.title || data.label}</span>

            {data.operationName && <pre className="text-sm">{data.operationName}</pre>}

            <span className="text-sm text-gray-500">{data.trigger ? 'trigger_1' : data.name}</span>
        </div>

        <Handle
            className={twMerge('left-node-handle-placement', styles.handle)}
            isConnectable={false}
            position={Position.Top}
            type="target"
        />

        <Handle
            className={twMerge('left-node-handle-placement', styles.handle)}
            isConnectable={false}
            position={Position.Bottom}
            type="source"
        />
    </div>
);

export default memo(ReadOnlyNode);
