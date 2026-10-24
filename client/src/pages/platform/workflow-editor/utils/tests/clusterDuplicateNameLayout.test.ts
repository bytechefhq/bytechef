import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {getClusterElementsLayoutElements} from '../layoutUtils';

const nodes: Node[] = [
    {
        data: {clusterElementTypesCount: 5, clusterElements: {tools: [{name: 'approvalGate_1'}]}},
        id: 'chat_1',
        position: {x: 0, y: 0},
        type: 'clusterRoot',
    },
    {
        data: {clusterElementType: 'tools', clusterElementTypeIndex: 4, isNestedClusterRoot: true},
        id: 'approvalGate_1',
        parentId: 'chat_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    },
    {
        data: {clusterElementType: 'approvalChannels', clusterElementTypeIndex: 1},
        id: 'chat_1',
        parentId: 'approvalGate_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    },
];

describe('getClusterElementsLayoutElements with a cluster element named like its root', () => {
    it('lays the nodes out instead of recursing without end', () => {
        const {nodes: layoutNodes} = getClusterElementsLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1200,
            edges: [],
            nodes,
        });

        expect(layoutNodes.map((node) => node.id)).toContain('approvalGate_1');
    });
});
