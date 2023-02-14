import React, {memo} from 'react';
import {Handle, Position, NodeProps} from 'reactflow';
import cx from 'classnames';

import styles from './NodeTypes.module.css';
import useNodeClickHandler from '../hooks/useNodeClick';

const WorkflowNode = ({id, data}: NodeProps) => {
    // see the hook implementation for details of the click handler
    // calling onClick adds a child node to this node
    const onClick = useNodeClickHandler(data, id);

    return (
        <div
            onClick={onClick}
            className={cx(
                styles.node,
                'radius-2 relative flex h-[72px] w-[72px] items-center justify-center rounded-md border-2 border-gray-300 bg-white shadow hover:bg-gray-200'
            )}
            title="Click to add a child node"
        >
            <div className="">{data.icon}</div>
            <div className="absolute left-[80px] flex w-full min-w-max flex-col items-start">
                <div className="text-sm text-gray-900">{data.label}</div>
                <div className="text-xs text-gray-500">{data.name}</div>
            </div>

            <Handle
                className={styles.handle}
                type="target"
                position={Position.Top}
                isConnectable={false}
            />

            <Handle
                className={styles.handle}
                type="source"
                position={Position.Bottom}
                isConnectable={false}
            />
        </div>
    );
};

export default memo(WorkflowNode);
