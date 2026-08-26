import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {shiftCanvasNodes} from '../useLayout';

function makeNode(id: string, x: number, parentId?: string): Node {
    return {data: {}, id, parentId, position: {x, y: 10}, type: 'workflow'};
}

describe('shiftCanvasNodes', () => {
    it('should shift a root-scope node by the given amount', () => {
        const [shifted] = shiftCanvasNodes([makeNode('task_1', 100)], 230);

        expect(shifted.position).toEqual({x: 330, y: 10});
    });

    it('should leave a graph frame member untouched so the frame shift is not applied twice', () => {
        const nodes = [makeNode('graph_1-graph-frame', 100), makeNode('accelo_1', 40, 'graph_1-graph-frame')];

        const [frame, member] = shiftCanvasNodes(nodes, 230);

        expect(frame.position.x).toBe(330);
        expect(member.position.x).toBe(40);
    });
});
