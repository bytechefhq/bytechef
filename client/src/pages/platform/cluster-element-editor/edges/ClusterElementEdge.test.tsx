import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {render, screen} from '@testing-library/react';
import {EdgeProps, Node, Position} from '@xyflow/react';
import {CSSProperties} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import ClusterElementEdge from './ClusterElementEdge';

const {testNodeStatesState} = vi.hoisted(() => ({
    testNodeStatesState: {value: {} as Record<string, {status: 'RUNNING' | 'COMPLETED' | 'FAILED'}>},
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useWorkflowTestNodeStates', () => ({
    default: () => testNodeStatesState.value,
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    BaseEdge: ({className, style}: {className?: string; style?: CSSProperties}) => (
        <svg>
            <path className={className} data-testid="cluster-element-edge" style={style} />
        </svg>
    ),
}));

const TOOL_NODE: Node = {
    data: {workflowNodeName: 'httpClient_1'},
    id: 'httpClient_1',
    position: {x: 0, y: 0},
    type: 'workflow',
};

function renderEdge() {
    return render(
        <ClusterElementEdge
            {...({
                id: 'aiAgent_1=>aiAgent_1-httpClient_1',
                source: 'aiAgent_1',
                sourcePosition: Position.Bottom,
                sourceX: 0,
                sourceY: 0,
                style: {fill: 'none', stroke: '#d1d5db', strokeWidth: 2},
                target: 'httpClient_1',
                targetPosition: Position.Top,
                targetX: 0,
                targetY: 100,
            } as EdgeProps)}
        />
    );
}

describe('ClusterElementEdge', () => {
    beforeEach(() => {
        useClusterElementsDataStore.setState({nodes: []});
        useWorkflowDataStore.setState({nodes: [TOOL_NODE]});

        testNodeStatesState.value = {};
    });

    it('colours the wire of a called element by its outcome and drops the inline grey stroke', () => {
        testNodeStatesState.value = {httpClient_1: {status: 'FAILED'}};

        renderEdge();

        const edge = screen.getByTestId('cluster-element-edge');

        expect(edge).toHaveClass('stroke-red-500');
        expect(edge.style.stroke).toBe('');
    });

    it('keeps the grey wire of an element that was not called', () => {
        renderEdge();

        const edge = screen.getByTestId('cluster-element-edge');

        expect(edge).not.toHaveClass('stroke-green-500');
        expect(edge).not.toHaveClass('stroke-red-500');
        expect(edge.style.stroke).not.toBe('');
    });
});
