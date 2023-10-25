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
                    'mx-[22px] flex cursor-pointer items-center justify-center rounded-md text-lg text-gray-500 shadow-none hover:scale-110 hover:bg-gray-500 hover:text-white',
                    isDropzoneActive
                        ? 'scale-150 cursor-pointer bg-blue-100 h-16 w-16 mx-1.5'
                        : 'h-7 w-7 bg-gray-300'
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
