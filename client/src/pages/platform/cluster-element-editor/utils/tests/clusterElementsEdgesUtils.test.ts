import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {createEdgeForMultipleClusterElementNode} from '../clusterElementsEdgesUtils';

describe('createEdgeForMultipleClusterElementNode', () => {
    it('draws the wire to a multiple-slot element with the colourable cluster element edge', () => {
        const edge = createEdgeForMultipleClusterElementNode('aiAgent_1', {
            data: {clusterElementType: 'tools'},
            id: 'httpClient_1',
            position: {x: 0, y: 0},
        } as Node);

        expect(edge).toMatchObject({
            id: 'aiAgent_1=>aiAgent_1-httpClient_1',
            sourceHandle: 'tools-handle',
            target: 'httpClient_1',
            type: 'clusterElement',
        });
    });
});
