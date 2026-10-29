import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import FitViewOnWorkflowChange from '../FitViewOnWorkflowChange';

const hoisted = vi.hoisted(() => ({
    fitViewMock: vi.fn(),
    nodesInitialized: {current: false},
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useNodesInitialized: () => hoisted.nodesInitialized.current,
    useReactFlow: () => ({fitView: hoisted.fitViewMock}),
}));

describe('FitViewOnWorkflowChange', () => {
    beforeEach(() => {
        hoisted.fitViewMock.mockClear();
        hoisted.nodesInitialized.current = false;

        useWorkflowDataStore.setState({
            workflow: {id: 'workflow-1', nodeNames: [], version: 1, workflowUuid: 'workflow-uuid-1'},
        });
    });

    it('fits the view once after the nodes are initialised', () => {
        const {rerender} = render(<FitViewOnWorkflowChange />);

        expect(hoisted.fitViewMock).not.toHaveBeenCalled();

        hoisted.nodesInitialized.current = true;

        rerender(<FitViewOnWorkflowChange />);
        rerender(<FitViewOnWorkflowChange />);

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(1);
    });

    it('fits the view again when the workflow changes', () => {
        hoisted.nodesInitialized.current = true;

        const {rerender} = render(<FitViewOnWorkflowChange />);

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(1);

        useWorkflowDataStore.setState({
            workflow: {id: 'workflow-2', nodeNames: [], version: 1, workflowUuid: 'workflow-uuid-2'},
        });

        rerender(<FitViewOnWorkflowChange />);

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(2);
    });

    it('fits the view again when the canvas width changes', () => {
        hoisted.nodesInitialized.current = true;

        const {rerender} = render(<FitViewOnWorkflowChange customCanvasWidth={600} />);

        rerender(<FitViewOnWorkflowChange customCanvasWidth={800} />);

        expect(hoisted.fitViewMock).toHaveBeenCalledTimes(2);
    });
});
