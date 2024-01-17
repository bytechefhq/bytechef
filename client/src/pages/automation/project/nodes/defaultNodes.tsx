import {PlayIcon} from 'lucide-react';
import {Node} from 'reactflow';

const defaultNodes: Node[] = [
    {
        data: {
            icon: <PlayIcon className="size-9 text-gray-700" />,
            label: 'Manual Trigger',
            name: 'manual',
            type: 'trigger',
        },
        id: '1',
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
