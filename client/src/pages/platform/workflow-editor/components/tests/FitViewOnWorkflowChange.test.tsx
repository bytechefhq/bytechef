import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useLayoutDirectionStore from '../../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import FitViewOnWorkflowChange from '../FitViewOnWorkflowChange';

const hoisted = vi.hoisted(() => ({
    fitViewMock: vi.fn(),
    flowState: {current: {height: 858, width: 1256}},
    nodesInitialized: {current: false},
    setViewportMock: vi.fn(),
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useNodesInitialized: () => hoisted.nodesInitialized.current,
    useReactFlow: () => ({
        fitView: hoisted.fitViewMock,
        getNodes: () => [
            {data: {}, id: 'trigger_1', position: {x: 100, y: 0}},
            {data: {}, id: 'trigger-placeholder', position: {x: 372, y: 12}},
        ],
        getNodesBounds: () => ({height: 400, width: 336, x: 100, y: 0}),
        setViewport: hoisted.setViewportMock,
    }),
    useStore: (selector: (state: {height: number; width: number}) => number) => selector(hoisted.flowState.current),
}));

describe('FitViewOnWorkflowChange', () => {
    beforeEach(() => {
        hoisted.fitViewMock.mockClear();
        hoisted.setViewportMock.mockClear();
        hoisted.flowState.current = {height: 858, width: 1256};
        hoisted.nodesInitialized.current = false;

        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

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

    it('centers the trigger column instead of the node bounds in the top-to-bottom layout', () => {
        hoisted.nodesInitialized.current = true;

        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        render(<FitViewOnWorkflowChange />);

        expect(hoisted.fitViewMock).not.toHaveBeenCalled();
        expect(hoisted.setViewportMock).toHaveBeenCalledWith({x: 628 - 136, y: 786 / 2 - 200 + 72, zoom: 1});
    });

    it('fits the view again when the flow is resized', () => {
        hoisted.nodesInitialized.current = true;

        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        const {rerender} = render(<FitViewOnWorkflowChange />);

        hoisted.flowState.current = {height: 858, width: 1600};

        rerender(<FitViewOnWorkflowChange />);

        expect(hoisted.setViewportMock).toHaveBeenCalledTimes(2);
        expect(hoisted.setViewportMock).toHaveBeenLastCalledWith(expect.objectContaining({x: 800 - 136}));
    });
});
