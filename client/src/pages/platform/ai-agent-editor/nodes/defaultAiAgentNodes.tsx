import {BotIcon} from 'lucide-react';

const defaultAiAgentNodes = [
    {
        data: {
            componentName: 'aiAgent',
            icon: <BotIcon className="size-9 text-black" />,
            id: 'aiAgent_1',
            label: 'AI Agent',
            name: 'aiAgent',
            type: 'aiAgent/v1/chat',
            workflowNodeName: 'aiAgent_1',
        },
        id: 'aiAgent_1',
        position: {x: 500, y: 250},
        type: 'workflow',
    },
    {
        data: {label: '+'},
        id: `aiAgent_1-rag-placeholder-0`,
        position: {x: 250, y: 500},
        type: 'placeholder',
    },
    {
        data: {label: '+'},
        id: `aiAgent_1-chatMemory-placeholder-0`,
        position: {x: 400, y: 500},
        type: 'placeholder',
    },
    {
        data: {label: '+'},
        id: `aiAgent_1-model-placeholder-0`,
        position: {x: 600, y: 500},
        type: 'placeholder',
    },
    {
        data: {label: '+'},
        id: `aiAgent_1-tools-placeholder-0`,
        position: {x: 750, y: 500},
        type: 'placeholder',
    },
];

export default defaultAiAgentNodes;
