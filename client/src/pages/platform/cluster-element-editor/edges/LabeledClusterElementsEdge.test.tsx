import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {render, screen} from '@testing-library/react';
import {EdgeProps, Node, Position} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';
import LabeledClusterElementsEdge from './LabeledClusterElementsEdge';

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    BaseEdge: () => null,
    EdgeLabelRenderer: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

const MODEL_PLACEHOLDER_NODE: Node = {
    data: {clusterElementLabel: 'Model'},
    id: 'aiAgent_1-model-placeholder-0',
    position: {x: 0, y: 0},
    type: 'placeholder',
};

function renderEdge() {
    const edgeProps = {
        id: 'aiAgent_1=>aiAgent_1-model-placeholder-0',
        source: 'aiAgent_1',
        sourcePosition: Position.Bottom,
        sourceX: 0,
        sourceY: 0,
        target: 'aiAgent_1-model-placeholder-0',
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
