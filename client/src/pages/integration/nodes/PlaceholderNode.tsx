import React, {memo} from 'react';
import {Handle, Position, NodeProps} from 'reactflow';

import styles from './NodeTypes.module.css';
import usePlaceholderClick from '../hooks/usePlaceholderClick';
import PopoverMenu from '../PopoverMenu';

const PlaceholderNode = ({id, data}: NodeProps) => {
    // see the hook implementation for details of the click handler
    // calling onClick turns this node and the connecting edge into a workflow node
    const onClick = usePlaceholderClick(id);

    return (
        <PopoverMenu>
            <div
                onClick={onClick}
                className="mx-[26px] flex h-[20px] w-[20px] cursor-pointer items-center justify-center rounded-md border border-gray-300 bg-gray-300 font-bold text-white shadow-none hover:bg-gray-500"
                title="Click to add a component"
            >
                {data.label}

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
        </PopoverMenu>
    );
};

export default memo(PlaceholderNode);
