import {Node} from '@xyflow/react';
import {PlayIcon} from 'lucide-react';

const defaultNodes: Node[] = [
    {
        data: {
            componentName: 'manual',
            icon: <PlayIcon className="size-9 text-gray-700" />,
            id: 'manual',
            label: 'Manual',
            name: 'manual',
            operationName: 'manual',
            trigger: true,
            type: 'manual/v1/manual',
            workflowNodeName: 'trigger_1',
        },
        id: 'trigger_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    },
    {
        data: {label: '+'},
        id: '2',
        position: {x: 0, y: 150},
        type: 'placeholder',
    },
];

export default defaultNodes;
