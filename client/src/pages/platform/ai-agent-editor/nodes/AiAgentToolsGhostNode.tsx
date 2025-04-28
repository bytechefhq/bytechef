import styles from '@/pages/platform/workflow-editor/nodes/NodeTypes.module.css';
import {Handle, Position} from '@xyflow/react';
import {memo} from 'react';
import {twMerge} from 'tailwind-merge';

const AiAgentToolsGhostNode = ({id}: {id: string}) => (
    <div className="h-0.5 w-[75px]" data-nodetype="aiAgentToolsGhostNode" key={id}>
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

export default memo(AiAgentToolsGhostNode);
