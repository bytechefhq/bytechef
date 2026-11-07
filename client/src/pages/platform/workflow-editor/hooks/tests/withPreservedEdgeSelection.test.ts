import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {Edge} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {withPreservedEdgeSelection} from '../useLayout';

interface TransitionEdgeProps {
    condition?: string;
    from: string;
    index: number;
    selected?: boolean;
    to: string;
}

function transitionEdge({condition, from, index, selected, to}: TransitionEdgeProps): Edge {
    return {
        data: {condition, graphId: 'graph_1', index, to},
        id: `graph_1-transition-${index}`,
        selected,
        source: from,
        target: to,
        type: GRAPH_TRANSITION_EDGE_TYPE,
    };
}

function selectedEdgeIds(edges: Edge[]): string[] {
    return edges.filter((edge) => edge.selected).map((edge) => edge.id);
}

describe('withPreservedEdgeSelection', () => {
    it('keeps a selected chain edge selected by id', () => {
        const previousEdges: Edge[] = [{id: 'task_1=>task_2', selected: true, source: 'task_1', target: 'task_2'}];
        const nextEdges: Edge[] = [{id: 'task_1=>task_2', source: 'task_1', target: 'task_2'}];

        expect(selectedEdgeIds(withPreservedEdgeSelection({nextEdges, previousEdges}))).toEqual(['task_1=>task_2']);
    });

    it('follows a selected transition to its new index after it is reordered', () => {
        const previousEdges = [
            transitionEdge({condition: '=a', from: 'draft', index: 0, selected: true, to: 'judge'}),
            transitionEdge({condition: '=b', from: 'draft', index: 1, to: 'publish'}),
        ];
        const nextEdges = [
            transitionEdge({condition: '=b', from: 'draft', index: 0, to: 'publish'}),
            transitionEdge({condition: '=a', from: 'draft', index: 1, to: 'judge'}),
        ];

        expect(selectedEdgeIds(withPreservedEdgeSelection({nextEdges, previousEdges}))).toEqual([
            'graph_1-transition-1',
        ]);
    });

    it('follows a selected transition when an earlier transition is removed', () => {
        const previousEdges = [
            transitionEdge({from: 'draft', index: 0, to: 'judge'}),
            transitionEdge({from: 'judge', index: 1, selected: true, to: 'publish'}),
        ];
        const nextEdges = [transitionEdge({from: 'judge', index: 0, to: 'publish'})];

        expect(selectedEdgeIds(withPreservedEdgeSelection({nextEdges, previousEdges}))).toEqual([
            'graph_1-transition-0',
        ]);
    });

    it('clears the selection when the selected transition itself is removed', () => {
        const previousEdges = [
            transitionEdge({from: 'draft', index: 0, selected: true, to: 'judge'}),
            transitionEdge({from: 'judge', index: 1, to: 'publish'}),
        ];
        const nextEdges = [transitionEdge({from: 'judge', index: 0, to: 'publish'})];

        expect(selectedEdgeIds(withPreservedEdgeSelection({nextEdges, previousEdges}))).toEqual([]);
    });

    it('keeps a transition selected in place while its condition is edited', () => {
        const previousEdges = [transitionEdge({condition: '=a', from: 'draft', index: 0, selected: true, to: 'judge'})];
        const nextEdges = [transitionEdge({condition: '=a && b', from: 'draft', index: 0, to: 'judge'})];

        expect(selectedEdgeIds(withPreservedEdgeSelection({nextEdges, previousEdges}))).toEqual([
            'graph_1-transition-0',
        ]);
    });
});
