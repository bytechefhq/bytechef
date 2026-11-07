import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {render, screen} from '@testing-library/react';
import {Edge, ReactFlowProvider, useStoreApi} from '@xyflow/react';
import {ReactNode, useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import GraphTransitionEditorLayer from './GraphTransitionEditorLayer';

vi.mock('@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionPopover', () => ({
    default: ({graphId, index}: {graphId: string; index: number}) => <div>{`editing ${graphId}[${index}]`}</div>,
}));

function ViewportHarness({children}: {children: ReactNode}) {
    const [domNodeReady, setDomNodeReady] = useState(false);

    const store = useStoreApi();

    return (
        <div
            ref={(node) => {
                if (node && !domNodeReady) {
                    const viewportPortal = document.createElement('div');

                    viewportPortal.className = 'react-flow__viewport-portal';

                    node.appendChild(viewportPortal);

                    store.setState({domNode: node});

                    setDomNodeReady(true);
                }
            }}
        >
            {domNodeReady && children}
        </div>
    );
}

function buildTransitionEdge(id: string, {index = 0, readOnly = false, selected = false} = {}): Edge {
    return {
        data: {graphId: 'graph_1', index, readOnly},
        id,
        selected,
        source: 'task_a',
        target: 'task_b',
        type: GRAPH_TRANSITION_EDGE_TYPE,
    };
}

function renderLayer(edges: Edge[], positions: Record<string, {labelX: number; labelY: number}>) {
    useWorkflowDataStore.setState({edges});
    useWorkflowEditorStore.setState({graphTransitionLabelPositions: positions});

    return render(
        <ReactFlowProvider>
            <ViewportHarness>
                <GraphTransitionEditorLayer />
            </ViewportHarness>
        </ReactFlowProvider>
    );
}

describe('GraphTransitionEditorLayer', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({edges: []});
        useWorkflowEditorStore.setState({graphTransitionLabelPositions: {}});
    });

    it('opens the editor on the transition its edge is selected for', () => {
        renderLayer([buildTransitionEdge('graph_1-transition-3', {index: 3, selected: true})], {
            'graph_1-transition-3': {labelX: 10, labelY: 20},
        });

        expect(screen.getByText('editing graph_1[3]')).toBeInTheDocument();
    });

    it('stays closed while no transition is selected', () => {
        renderLayer([buildTransitionEdge('graph_1-transition-0')], {'graph_1-transition-0': {labelX: 10, labelY: 20}});

        expect(screen.queryByText('editing graph_1[0]')).not.toBeInTheDocument();
    });

    it('stays closed on a read-only canvas', () => {
        renderLayer([buildTransitionEdge('graph_1-transition-0', {readOnly: true, selected: true})], {
            'graph_1-transition-0': {labelX: 10, labelY: 20},
        });

        expect(screen.queryByText('editing graph_1[0]')).not.toBeInTheDocument();
    });

    it('stands down while more than one transition is selected', () => {
        renderLayer(
            [
                buildTransitionEdge('graph_1-transition-0', {index: 0, selected: true}),
                buildTransitionEdge('graph_1-transition-1', {index: 1, selected: true}),
            ],
            {
                'graph_1-transition-0': {labelX: 10, labelY: 20},
                'graph_1-transition-1': {labelX: 30, labelY: 40},
            }
        );

        expect(screen.queryByText('editing graph_1[0]')).not.toBeInTheDocument();
        expect(screen.queryByText('editing graph_1[1]')).not.toBeInTheDocument();
    });

    it('waits for the edge to publish where it hangs', () => {
        renderLayer([buildTransitionEdge('graph_1-transition-0', {selected: true})], {});

        expect(screen.queryByText('editing graph_1[0]')).not.toBeInTheDocument();
    });

    it('keeps the editor mounted across a relayout that replaces every edge object', () => {
        const {rerender} = renderLayer([buildTransitionEdge('graph_1-transition-0', {selected: true})], {
            'graph_1-transition-0': {labelX: 10, labelY: 20},
        });

        const editorBefore = screen.getByText('editing graph_1[0]');

        useWorkflowDataStore.setState({edges: [buildTransitionEdge('graph_1-transition-0', {selected: true})]});

        rerender(
            <ReactFlowProvider>
                <ViewportHarness>
                    <GraphTransitionEditorLayer />
                </ViewportHarness>
            </ReactFlowProvider>
        );

        expect(screen.getByText('editing graph_1[0]')).toBe(editorBefore);
    });
});
