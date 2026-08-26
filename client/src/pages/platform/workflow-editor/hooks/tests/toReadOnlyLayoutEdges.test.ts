import {GraphTransitionEdgeDataI} from '@/pages/platform/workflow-editor/edges/GraphTransitionEdge';
import {Edge} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {toReadOnlyLayoutEdges} from '../useLayout';

const transitionEdge: Edge = {
    data: {condition: '=x > 1', dangling: false, dynamic: false, graphId: 'graph_1', index: 0, to: 'taskC'},
    id: 'graph_1-transition-0',
    source: 'taskA',
    sourceHandle: 'taskA-graph-transition-source',
    target: 'taskC',
    targetHandle: 'taskC-graph-transition-target',
    type: 'graphTransition',
};

const startEdge: Edge = {
    id: 'graph_1-graph-start=>taskA',
    reconnectable: 'target',
    source: 'graph_1-graph-start',
    sourceHandle: 'graph_1-graph-start-source',
    target: 'taskA',
    targetHandle: 'taskA-graph-transition-target',
    type: 'graphStart',
};

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

    it('should keep a graph transition as a graphTransition edge rather than dropping or downgrading it', () => {
        const result = toReadOnlyLayoutEdges([
            {id: 'a=>b', source: 'a', target: 'b', type: 'workflow'},
            transitionEdge,
        ]);

        expect(result).toHaveLength(2);

        const readOnlyTransition = result.find((edge) => edge.id === 'graph_1-transition-0');

        expect(readOnlyTransition?.type).toBe('graphTransition');
        expect(readOnlyTransition?.sourceHandle).toBe('taskA-graph-transition-source');
        expect(readOnlyTransition?.targetHandle).toBe('taskC-graph-transition-target');
    });

    it('should keep a transition edge paintable by preserving the data its styling reads', () => {
        const [readOnlyTransition] = toReadOnlyLayoutEdges([
            {...transitionEdge, data: {...transitionEdge.data, dynamic: true, to: '=nextStep'}},
        ]);

        const data = readOnlyTransition.data as unknown as GraphTransitionEdgeDataI;

        expect(data.dynamic).toBe(true);
        expect(data.to).toBe('=nextStep');
        expect(data.graphId).toBe('graph_1');
    });

    it('should mark a transition read-only so it cannot open its editor', () => {
        const [readOnlyTransition] = toReadOnlyLayoutEdges([transitionEdge]);

        expect((readOnlyTransition.data as unknown as GraphTransitionEdgeDataI).readOnly).toBe(true);
        expect((transitionEdge.data as unknown as GraphTransitionEdgeDataI).readOnly).toBeUndefined();
    });

    it('should keep the start edge as a graphStart edge, no longer re-pointable', () => {
        const [readOnlyStart] = toReadOnlyLayoutEdges([startEdge]);

        expect(readOnlyStart.type).toBe('graphStart');
        expect(readOnlyStart.reconnectable).toBe(false);
    });

    it('should keep a graph-only edge list intact', () => {
        expect(toReadOnlyLayoutEdges([transitionEdge, startEdge]).map((edge) => edge.type)).toEqual([
            'graphTransition',
            'graphStart',
        ]);
    });
});
