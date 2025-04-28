import {ClusterElementItemType} from '@/shared/types';
import {Node} from '@xyflow/react';

export function createPlaceholderNode(currentAiAgentNodeName: string | undefined, type: string): Node {
    return {
        data: {clusterElementType: type, label: '+'},
        id: `${currentAiAgentNodeName}-${type}-placeholder-0`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };
}

export function createToolsGhostNode(currentAiAgentNodeName: string | undefined): Node {
    return {
        data: {
            aiAgentId: currentAiAgentNodeName,
            clusterElementType: 'tools',
        },
        id: `${currentAiAgentNodeName}-tools-ghost`,
        position: {x: 0, y: 0},
        type: 'aiAgentToolsGhostNode',
    };
}

export function createClusterElementNode(clusterElementData: ClusterElementItemType) {
    const {label, name, parameters, type} = clusterElementData;

    return {
        data: {
            ...clusterElementData,
            clusterElementType: type.split('/')[2],
            label,
            name,
            parameters,
            type,
        },
        id: name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

export function createToolNode(tool: ClusterElementItemType) {
    const {label, name, parameters, type} = tool;

    return {
        data: {
            ...tool,
            clusterElementType: 'tools',
            label,
            name,
            parameters,
            type,
        },
        id: name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}
