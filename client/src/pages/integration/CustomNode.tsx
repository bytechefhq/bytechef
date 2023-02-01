import React, {memo} from 'react';
import {Handle, NodeProps, Position} from 'reactflow';

const CustomNode = ({data}: NodeProps) => (
    <div className="rounded-md bg-slate-100 shadow-md">
        <div className="px-4 py-2 font-bold text-gray-600">{data.label}</div>

        <Handle
            type="target"
            position={Position.Top}
            className="rounded-md bg-gray-500"
        />

        <Handle
            type="source"
            position={Position.Bottom}
            className="rounded-md bg-gray-500"
        />
    </div>
);

export default memo(CustomNode);
