import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {render, screen} from '@testing-library/react';
import {EdgeProps, Node, Position} from '@xyflow/react';
import {CSSProperties, ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import LabeledClusterElementsEdge from './LabeledClusterElementsEdge';

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
    EdgeLabelRenderer: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

const MODEL_PLACEHOLDER_NODE: Node = {
    data: {clusterElementLabel: 'Model'},
    id: 'aiAgent_1-model-placeholder-0',
    position: {x: 0, y: 0},
    type: 'placeholder',
};

const TOOL_NODE: Node = {
    data: {clusterElementLabel: 'Tools', workflowNodeName: 'httpClient_1'},
    id: 'httpClient_1',
    position: {x: 0, y: 0},
    type: 'workflow',
};

function renderEdge(target = 'aiAgent_1-model-placeholder-0') {
    const edgeProps = {
        id: `aiAgent_1=>${target}`,
        source: 'aiAgent_1',
        sourcePosition: Position.Bottom,
        sourceX: 0,
        sourceY: 0,
        style: {fill: 'none', stroke: '#d1d5db', strokeWidth: 2},
        target,
        targetPosition: Position.Top,
        targetX: 0,
        targetY: 100,
    } as EdgeProps;

    return render(<LabeledClusterElementsEdge {...edgeProps} />);
}

describe('LabeledClusterElementsEdge', () => {
    beforeEach(() => {
        useClusterElementsDataStore.setState({nodes: []});
        useWorkflowDataStore.setState({nodes: []});

        testNodeStatesState.value = {};
    });

    it("colours a tools edge by the called element's status", () => {
        testNodeStatesState.value = {httpClient_1: {status: 'COMPLETED'}};

        useWorkflowDataStore.setState({nodes: [TOOL_NODE]});

        renderEdge('aiAgent_1-httpClient_1');

        expect(screen.getByTestId('cluster-element-edge')).toHaveClass('stroke-green-500');
        expect(screen.getByTestId('cluster-element-edge').style.stroke).toBe('');
    });

    it('colours a tools edge red when the called element failed', () => {
        testNodeStatesState.value = {httpClient_1: {status: 'FAILED'}};

        useWorkflowDataStore.setState({nodes: [TOOL_NODE]});

        renderEdge('aiAgent_1-httpClient_1');

        expect(screen.getByTestId('cluster-element-edge')).toHaveClass('stroke-red-500');
    });

    it('leaves a model edge uncoloured', () => {
        testNodeStatesState.value = {httpClient_1: {status: 'COMPLETED'}};

        useWorkflowDataStore.setState({nodes: [MODEL_PLACEHOLDER_NODE]});

        renderEdge();

        const edge = screen.getByTestId('cluster-element-edge');

        expect(edge).not.toHaveClass('stroke-green-500');
        expect(edge).not.toHaveClass('stroke-red-500');
    });

    it('labels an edge whose target sits on the main workflow canvas', () => {
        useWorkflowDataStore.setState({nodes: [MODEL_PLACEHOLDER_NODE]});

        renderEdge();

        expect(screen.getByText('Model')).toBeInTheDocument();
    });

    it('labels an edge whose target sits in the cluster element editor dialog', () => {
        useClusterElementsDataStore.setState({nodes: [MODEL_PLACEHOLDER_NODE]});

        renderEdge();

        expect(screen.getByText('Model')).toBeInTheDocument();
    });

    it('leaves the edge unlabelled when its target is on neither canvas', () => {
        renderEdge();

        expect(screen.queryByText('Model')).not.toBeInTheDocument();
    });
});
