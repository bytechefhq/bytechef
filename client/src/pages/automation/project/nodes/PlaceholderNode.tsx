import React, {memo} from 'react';
import {Handle, Position, NodeProps} from 'reactflow';

import styles from './NodeTypes.module.css';
import PopoverMenu from '../components/PopoverMenu';

const PlaceholderNode = ({id, data}: NodeProps) => (
    <PopoverMenu id={id}>
        <div
            // eslint-disable-next-line tailwindcss/no-custom-classname
            className="mx-[24px] flex h-6 w-6 cursor-pointer items-center justify-center rounded-md bg-gray-300 font-bold text-white shadow-none hover:scale-110 hover:rounded-sm hover:bg-gray-500"
            title="Click to add a component"
        >
            <span className="text-lg">{data.label}</span>

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

export default memo(PlaceholderNode);
