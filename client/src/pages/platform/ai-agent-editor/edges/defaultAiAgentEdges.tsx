const defaultAiAgentEdges = [
    {
        id: `aiAgent_1=>aiAgent_1-rag-placeholder-0`,
        source: 'aiAgent_1',
        target: `aiAgent_1-rag-placeholder-0`,
        type: 'smoothstep',
    },
    {
        id: `aiAgent_1=>aiAgent_1-chatMemory-placeholder-0`,
        source: 'aiAgent_1',
        target: `aiAgent_1-chatMemory-placeholder-0`,
        type: 'smoothstep',
    },
    {
        id: `aiAgent_1=>aiAgent_1-model-placeholder-0`,
        source: 'aiAgent_1',
        target: `aiAgent_1-model-placeholder-0`,
        type: 'smoothstep',
    },
    {
        id: `aiAgent_1=>aiAgent_1-tools-placeholder-0`,
        source: 'aiAgent_1',
        target: `aiAgent_1-tools-placeholder-0`,
        type: 'smoothstep',
    },
];

export default defaultAiAgentEdges;
