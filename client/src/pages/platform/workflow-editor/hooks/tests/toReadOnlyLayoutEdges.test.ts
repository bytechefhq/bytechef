import {Edge} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {toReadOnlyLayoutEdges} from '../useLayout';

describe('toReadOnlyLayoutEdges', () => {
    it('should rewrite structural edges to smoothstep', () => {
        const edges: Edge[] = [
            {id: 'a=>b', source: 'a', target: 'b', type: 'workflow'},
            {id: 'b=>c', source: 'b', target: 'c', type: 'placeholder'},
        ];

        const result = toReadOnlyLayoutEdges(edges);

        expect(result).toHaveLength(2);
        expect(result.every((edge) => edge.type === 'smoothstep')).toBe(true);
    });

    it('should drop graphTransition edges entirely rather than rewriting them', () => {
        const edges: Edge[] = [
            {id: 'a=>b', source: 'a', target: 'b', type: 'workflow'},
            {
                id: 'graph_1-transition-0-1',
                source: 'taskA',
                sourceHandle: 'taskA-graph-transition-source',
                target: 'taskC',
                targetHandle: 'taskC-graph-transition-target',
                type: 'graphTransition',
            },
        ];

        const result = toReadOnlyLayoutEdges(edges);

        expect(result).toHaveLength(1);
        expect(result.some((edge) => edge.type === 'graphTransition')).toBe(false);
        expect(result[0].id).toBe('a=>b');
    });

    it('should return an empty array when only graphTransition edges are present', () => {
        const edges: Edge[] = [
            {
                id: 'graph_1-transition-0-0',
                source: 'taskA',
                target: 'taskA',
                type: 'graphTransition',
            },
        ];

        expect(toReadOnlyLayoutEdges(edges)).toEqual([]);
    });
});
