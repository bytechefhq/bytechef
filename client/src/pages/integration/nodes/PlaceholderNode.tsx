import React, {memo} from 'react';
import {Handle, Position, NodeProps} from 'reactflow';

import styles from './NodeTypes.module.css';
import PopoverMenu from '../components/PopoverMenu';

const PlaceholderNode = ({id, data}: NodeProps) => (
    <PopoverMenu id={id}>
        <div
            // eslint-disable-next-line tailwindcss/no-custom-classname
            className="mx-[26px] flex h-5 w-5 cursor-pointer items-center justify-center rounded-md bg-gray-300 font-bold text-white shadow-none hover:scale-110 hover:rounded-sm hover:bg-gray-500"
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

export default memo(PlaceholderNode);
