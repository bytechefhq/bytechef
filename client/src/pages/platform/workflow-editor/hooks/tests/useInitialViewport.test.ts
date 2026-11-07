import useInitialViewport, {
    CANVAS_LEFT_INSET,
    CANVAS_TOP_INSET,
} from '@/pages/platform/workflow-editor/hooks/useInitialViewport';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {flowState, setViewportMock} = vi.hoisted(() => ({
    flowState: {
        height: 0,
        nodes: [] as Array<{parentId?: string; type: string}>,
        nodesInitialized: false,
        viewport: {x: 0, y: 0, zoom: 1},
    },
    setViewportMock: vi.fn(),
}));

const NODES_BOUNDS = {height: 100, width: 200, x: 0, y: 10};

vi.mock('@xyflow/react', () => ({
    useNodesInitialized: () => flowState.nodesInitialized,
    useReactFlow: () => ({
        getNodes: () => flowState.nodes,
        getNodesBounds: () => NODES_BOUNDS,
        getViewport: () => flowState.viewport,
        setViewport: setViewportMock,
    }),
    useStore: (selector: (state: {height: number}) => number) => selector({height: flowState.height}),
    useStoreApi: () => ({getState: () => ({height: flowState.height})}),
}));

interface RenderPropsI {
    enabled: boolean;
    workflowId?: string;
}

const getViewportOffsetX = () => 0;

const renderInitialViewport = ({enabled = true, workflowId = 'workflow-1'}: Partial<RenderPropsI> = {}) =>
    renderHook((props: RenderPropsI) => useInitialViewport({getViewportOffsetX, ...props}), {
        initialProps: {enabled, workflowId},
    });

describe('useInitialViewport', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        flowState.height = 700;
        flowState.nodes = [{type: 'workflow'}];
        flowState.nodesInitialized = true;
        flowState.viewport = {x: 0, y: 0, zoom: 1};

        useLayoutDirectionStore.setState({layoutDirection: 'TB'});
    });

    it('waits for the nodes to be measured before positioning', () => {
        flowState.nodesInitialized = false;

        renderInitialViewport();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('waits for a real node rather than anchoring to a placeholder', () => {
        flowState.nodes = [{type: 'placeholder'}];

        renderInitialViewport();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('places the topmost node the top inset below the canvas edge, without animating the first placement', () => {
        renderInitialViewport();

        expect(setViewportMock).toHaveBeenCalledTimes(1);
        expect(setViewportMock).toHaveBeenCalledWith(
            {x: 0, y: CANVAS_TOP_INSET - NODES_BOUNDS.y, zoom: 1},
            {duration: 0}
        );
    });

    it('places a left-to-right graph by its left edge and centres it vertically', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        renderInitialViewport();

        expect(setViewportMock).toHaveBeenCalledWith(
            {x: CANVAS_LEFT_INSET - NODES_BOUNDS.x, y: 700 / 2 - (NODES_BOUNDS.y + NODES_BOUNDS.height / 2), zoom: 1},
            {duration: 0}
        );
    });

    it('positions once per workflow, animating when another workflow opens', () => {
        const {rerender} = renderInitialViewport();

        rerender({enabled: true, workflowId: 'workflow-1'});

        expect(setViewportMock).toHaveBeenCalledTimes(1);

        rerender({enabled: true, workflowId: 'workflow-2'});

        expect(setViewportMock).toHaveBeenCalledTimes(2);
        expect(setViewportMock).toHaveBeenLastCalledWith(expect.anything(), {duration: 500});
    });

    it('does nothing without a workflow id or when disabled', () => {
        renderInitialViewport({workflowId: ''});
        renderInitialViewport({enabled: false});

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('keeps a left-to-right graph centred when the canvas height changes', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        const {rerender} = renderInitialViewport();

        setViewportMock.mockClear();

        flowState.height = 500;

        rerender({enabled: true, workflowId: 'workflow-1'});

        expect(setViewportMock).toHaveBeenCalledTimes(1);
    });

    it('leaves a top-to-bottom graph alone when the canvas height changes', () => {
        const {rerender} = renderInitialViewport();

        setViewportMock.mockClear();

        flowState.height = 500;

        rerender({enabled: true, workflowId: 'workflow-1'});

        expect(setViewportMock).not.toHaveBeenCalled();
    });
});
