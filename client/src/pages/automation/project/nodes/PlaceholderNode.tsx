import {memo, useState} from 'react';
import {Handle, NodeProps, Position} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import PopoverMenu from '../components/PopoverMenu';
import styles from './NodeTypes.module.css';

const PlaceholderNode = ({data, id}: NodeProps) => {
    const [isDropzoneActive, setDropzoneActive] = useState(false);

    return (
        <PopoverMenu id={id}>
            <div
                className={twMerge(
                    'mx-[20px] flex h-8 w-8 cursor-pointer items-center justify-center rounded-md bg-gray-300 text-lg font-bold text-white shadow-none hover:scale-110 hover:rounded-sm hover:bg-gray-500',
                    isDropzoneActive && 'scale-150 cursor-pointer bg-gray-500'
                )}
                title="Click to add a node"
                onDrop={() => setDropzoneActive(false)}
                onDragOver={(event) => event.preventDefault()}
                onDragEnter={() => setDropzoneActive(true)}
                onDragLeave={() => setDropzoneActive(false)}
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
