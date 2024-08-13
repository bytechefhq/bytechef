import {Button} from '@/components/ui/button';
import {memo} from 'react';
import {Handle, NodeProps, Position} from 'reactflow';
import {twMerge} from 'tailwind-merge';

import useNodeClickHandler from '../hooks/useNodeClick';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import styles from './NodeTypes.module.css';

const ConditionNode = ({data, id}: NodeProps) => {
    const {currentNode, workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();

    const isSelected = currentNode?.name === data.name;

    const handleNodeClick = useNodeClickHandler(data, id);

    return (
        <div className="relative flex min-w-[240px] cursor-pointer items-center justify-center">
            <Button
                className={twMerge(
                    'h-18 w-18 rounded-md border-2 border-gray-300 bg-white p-4 shadow hover:border-blue-200 hover:bg-blue-200 hover:shadow-none',
                    isSelected && workflowNodeDetailsPanelOpen && 'border-blue-300 bg-blue-100 shadow-none'
                )}
                onClick={handleNodeClick}
            >
                {data.icon}
            </Button>

            <div className="ml-2 flex w-full min-w-max flex-col items-start">
                <span className="font-semibold">{data.title || data.label}</span>

                <span className="text-sm text-gray-500">{data.name}</span>
            </div>

            <span className="absolute -left-20 top-3 px-2 py-0.5 text-sm font-bold uppercase text-gray-500">True</span>

            <span className="absolute left-72 top-3 px-2 py-0.5 text-sm font-bold uppercase text-gray-500">False</span>

            <Handle
                className={twMerge('left-[36px]', styles.handle)}
                isConnectable={false}
                position={Position.Top}
                type="target"
            />

            <Handle id="left" isConnectable={false} position={Position.Left} type="source" />

            <Handle id="right" isConnectable={false} position={Position.Right} type="source" />
        </div>
    );
};

export default memo(ConditionNode);
