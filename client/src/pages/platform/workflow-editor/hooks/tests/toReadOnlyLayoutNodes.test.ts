import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {toReadOnlyLayoutNodes} from '../useLayout';

describe('toReadOnlyLayoutNodes', () => {
    it('should clear a graph member own draggable flag, which outranks the canvas-wide lock', () => {
        // What `layoutGraphFrames` stamps on every direct frame member.
        const member: Node = {
            data: {},
            draggable: true,
            extent: 'parent',
            id: 'taskA',
            parentId: 'graph_1-graph-frame',
            position: {x: 20, y: 20},
            type: 'readonly',
        };

        const [readOnlyMember] = toReadOnlyLayoutNodes([member]);

        expect(readOnlyMember.draggable).toBe(false);
        expect(readOnlyMember.parentId).toBe('graph_1-graph-frame');
        expect(readOnlyMember.position).toEqual({x: 20, y: 20});
    });

    it('should leave a node that never declared draggable untouched, so the canvas-wide lock decides', () => {
        const node: Node = {data: {}, id: 'var_1', position: {x: 0, y: 0}, type: 'readonly'};

        const [readOnlyNode] = toReadOnlyLayoutNodes([node]);

        expect(readOnlyNode.draggable).toBeUndefined();
        expect(readOnlyNode).toBe(node);
    });

    it('should not mutate the nodes it was given', () => {
        const member: Node = {data: {}, draggable: true, id: 'taskA', position: {x: 0, y: 0}};

        toReadOnlyLayoutNodes([member]);

        expect(member.draggable).toBe(true);
    });
});
