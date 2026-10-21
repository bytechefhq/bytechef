import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import shiftNodesHorizontally from './shiftNodesHorizontally';

function createNode(id: string, x: number, y: number, parentId?: string): Node {
    return {
        data: {},
        id,
        parentId,
        position: {x, y},
        type: 'workflow',
    };
}

describe('shiftNodesHorizontally', () => {
    it('shifts top-level nodes by the given amount', () => {
        const shiftedNodes = shiftNodesHorizontally(
            [createNode('trigger_1', 100, 0), createNode('aiAgent_1', 40, 200)],
            -177.5
        );

        expect(shiftedNodes.map((node) => node.position)).toEqual([
            {x: -77.5, y: 0},
            {x: -137.5, y: 200},
        ]);
    });

    it('keeps parented nodes at their parent-relative positions so they move with their parent', () => {
        const shiftedNodes = shiftNodesHorizontally(
            [
                createNode('aiAgent_1', 40, 200),
                createNode('anthropic_1', 81, 293, 'aiAgent_1'),
                createNode('inMemoryChatMemory_1', 193, 293, 'aiAgent_1'),
                createNode('inMemoryChatMemory_1-model-placeholder-0', 128, 140, 'inMemoryChatMemory_1'),
            ],
            -177.5
        );

        expect(shiftedNodes.map((node) => node.position)).toEqual([
            {x: -137.5, y: 200},
            {x: 81, y: 293},
            {x: 193, y: 293},
            {x: 128, y: 140},
        ]);
    });
});
