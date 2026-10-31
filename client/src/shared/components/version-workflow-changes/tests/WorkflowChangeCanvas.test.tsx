import useLayoutEngineStore from '@/pages/platform/workflow-editor/stores/useLayoutEngineStore';
import {act, render, waitFor} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowChangeCanvas from '../WorkflowChangeCanvas';

const mocks = vi.hoisted(() => ({
    buildWorkflowGraph: vi.fn(),
    fitView: vi.fn(),
    getElkLayoutElements: vi.fn(),
    getLayoutElements: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/utils/buildWorkflowGraph', () => ({default: mocks.buildWorkflowGraph}));

vi.mock('@/pages/platform/workflow-editor/utils/layoutUtils', () => ({
    getLayoutElements: mocks.getLayoutElements,
}));

vi.mock('@/pages/platform/workflow-editor/utils/elkLayoutUtils', () => ({
    getElkLayoutElements: mocks.getElkLayoutElements,
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    Background: () => null,
    ReactFlow: ({children}: {children: ReactNode}) => <div data-testid="flow">{children}</div>,
    useReactFlow: () => ({fitView: mocks.fitView}),
}));

const workflow = {tasks: [], triggers: []} as never;

const renderCanvas = () =>
    render(
        <WorkflowChangeCanvas
            componentDefinitions={[]}
            highlights={{}}
            taskDispatcherDefinitions={[]}
            workflow={workflow}
        />
    );

describe('WorkflowChangeCanvas', () => {
    beforeEach(() => {
        mocks.buildWorkflowGraph.mockReset();
        mocks.fitView.mockReset();
        mocks.getElkLayoutElements.mockReset();
        mocks.getLayoutElements.mockReset();

        useLayoutEngineStore.setState({layoutEngine: 'dagre'});
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('refits the view with the editor preview options once a layout produced nodes', async () => {
        const layoutNode = {data: {}, id: 'a', position: {x: 0, y: 0}, type: 'readonly'};

        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [],
            nodes: [{data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'}],
        });
        mocks.getLayoutElements.mockResolvedValue({edges: [], nodes: [layoutNode]});

        renderCanvas();

        await waitFor(() => expect(mocks.fitView).toHaveBeenCalledWith({maxZoom: 1, minZoom: 0.1, padding: 0.15}));
    });

    it('does not refit an empty canvas', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [],
            nodes: [{data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'}],
        });
        mocks.getLayoutElements.mockResolvedValue({edges: [], nodes: []});

        renderCanvas();

        await waitFor(() => expect(mocks.getLayoutElements).toHaveBeenCalled());

        expect(mocks.fitView).not.toHaveBeenCalled();
    });

    it('lays the graph out again at the measured canvas width', async () => {
        let resizeCallback: ResizeObserverCallback = () => {};

        vi.stubGlobal(
            'ResizeObserver',
            class {
                constructor(callback: ResizeObserverCallback) {
                    resizeCallback = callback;
                }

                disconnect() {}

                observe() {}
            }
        );

        mocks.buildWorkflowGraph.mockReturnValue({edges: [], nodes: []});

        renderCanvas();

        await waitFor(() =>
            expect(mocks.buildWorkflowGraph).toHaveBeenCalledWith(expect.objectContaining({canvasWidth: 756}))
        );

        act(() => {
            resizeCallback([{contentRect: {width: 0}} as ResizeObserverEntry], {} as ResizeObserver);
        });

        expect(mocks.buildWorkflowGraph).not.toHaveBeenCalledWith(expect.objectContaining({canvasWidth: 0}));

        act(() => {
            resizeCallback([{contentRect: {width: 1200}} as ResizeObserverEntry], {} as ResizeObserver);
        });

        await waitFor(() =>
            expect(mocks.buildWorkflowGraph).toHaveBeenCalledWith(expect.objectContaining({canvasWidth: 1200}))
        );
    });

    it('lays out with ELK when the editor uses the ELK engine', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [],
            nodes: [{data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'}],
        });
        mocks.getElkLayoutElements.mockResolvedValue({edges: [], nodes: []});

        useLayoutEngineStore.setState({layoutEngine: 'elk'});

        renderCanvas();

        await waitFor(() => expect(mocks.getElkLayoutElements).toHaveBeenCalled());

        expect(mocks.getLayoutElements).not.toHaveBeenCalled();
    });

    it('skips the layout when only placeholder nodes remain', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [{id: 'e1', source: 'trigger-placeholder', target: 'x'}],
            nodes: [{data: {}, id: 'trigger-placeholder', position: {x: 0, y: 0}, type: 'placeholder'}],
        });

        renderCanvas();

        await waitFor(() => expect(mocks.buildWorkflowGraph).toHaveBeenCalled());

        expect(mocks.getLayoutElements).not.toHaveBeenCalled();
    });

    it('does not reject when the layout fails', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [],
            nodes: [{data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'}],
        });
        mocks.getLayoutElements.mockRejectedValue(new Error('boom'));

        renderCanvas();

        await waitFor(() => expect(mocks.getLayoutElements).toHaveBeenCalled());
    });

    it('drops edges that reference removed placeholder nodes', async () => {
        mocks.buildWorkflowGraph.mockReturnValue({
            edges: [
                {id: 'e1', source: 'trigger-placeholder', target: 'a'},
                {id: 'e2', source: 'a', target: 'b'},
            ],
            nodes: [
                {data: {}, id: 'trigger-placeholder', position: {x: 0, y: 0}, type: 'placeholder'},
                {data: {}, id: 'a', position: {x: 0, y: 0}, type: 'workflow'},
                {data: {}, id: 'b', position: {x: 0, y: 0}, type: 'workflow'},
            ],
        });
        mocks.getLayoutElements.mockResolvedValue({edges: [], nodes: []});

        renderCanvas();

        await waitFor(() => expect(mocks.getLayoutElements).toHaveBeenCalled());

        const {edges, nodes} = mocks.getLayoutElements.mock.calls[0][0];
        const nodeIds = nodes.map((node: {id: string}) => node.id);

        expect(edges.map((edge: {id: string}) => edge.id)).toEqual(['e2']);
        edges.forEach((edge: {source: string; target: string}) => {
            expect(nodeIds).toContain(edge.source);
            expect(nodeIds).toContain(edge.target);
        });
    });
});
