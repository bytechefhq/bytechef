import useInitialViewport from '@/pages/platform/workflow-editor/hooks/useInitialViewport';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET} from '@/shared/constants';
import {act, renderHook} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {fitViewMock, flowState, setViewportMock} = vi.hoisted(() => ({
    fitViewMock: vi.fn(),
    flowState: {
        bounds: {height: 400, width: 600, x: 0, y: 0},
        height: 0,
        nodesInitialized: true,
        viewport: {x: 0, y: 25, zoom: 1},
        width: 1200,
    },
    setViewportMock: vi.fn(),
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useNodesInitialized: () => flowState.nodesInitialized,
    useReactFlow: () => ({
        fitView: fitViewMock,
        getNodesBounds: () => flowState.bounds,
        getViewport: () => flowState.viewport,
        setViewport: setViewportMock,
    }),
    useStore: (selector: (state: {height: number; width: number}) => number) =>
        selector({height: flowState.height, width: flowState.width}),
}));

const toNode = (id: string): Node => ({data: {}, id, position: {x: 0, y: 0}});

const setLaidOutNodes = (nodeIds: string[]) => {
    act(() => {
        useWorkflowDataStore.setState({nodes: nodeIds.map(toNode)});
    });
};

interface RenderPropsI {
    enabled: boolean;
    workflowUuid?: string;
}

const renderInitialViewport = ({enabled = true, workflowUuid = 'workflow-1'}: Partial<RenderPropsI> = {}) =>
    renderHook(
        (props: RenderPropsI) => useInitialViewport({canvasHeight: 650, getViewportOffsetX: () => 0, ...props}),
        {
            initialProps: {enabled, workflowUuid},
        }
    );

describe('useInitialViewport', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        flowState.bounds = {height: 400, width: 600, x: 0, y: 0};
        flowState.height = 0;
        flowState.nodesInitialized = true;
        flowState.viewport = {x: 0, y: 25, zoom: 1};

        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        useWorkflowDataStore.setState((state) => ({
            nodes: [],
            workflow: {...state.workflow, definition: JSON.stringify({tasks: [{name: 'aiAgent_1'}]})},
        }));
    });

    it('waits for the flow height before positioning', () => {
        renderInitialViewport();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('positions once the flow height is measured', () => {
        const {rerender} = renderInitialViewport();

        flowState.height = 700;

        rerender({enabled: true, workflowUuid: 'workflow-1'});

        expect(setViewportMock).toHaveBeenCalledTimes(1);
        expect(setViewportMock).toHaveBeenCalledWith({x: CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET, y: 25, zoom: 1});
    });

    it('leaves the viewport alone on later resizes of the same workflow', () => {
        flowState.height = 700;

        const {rerender} = renderInitialViewport();

        flowState.height = 900;

        rerender({enabled: true, workflowUuid: 'workflow-1'});

        expect(setViewportMock).toHaveBeenCalledTimes(1);
    });

    it('positions again when another workflow opens', () => {
        flowState.height = 700;

        const {rerender} = renderInitialViewport();

        rerender({enabled: true, workflowUuid: 'workflow-2'});

        expect(setViewportMock).toHaveBeenCalledTimes(2);
    });

    it('positions a workflow without a uuid', () => {
        flowState.height = 700;

        renderHook(() => useInitialViewport({canvasHeight: 650, enabled: true, getViewportOffsetX: () => 0}));

        expect(setViewportMock).toHaveBeenCalledTimes(1);
    });

    it('does nothing when disabled', () => {
        flowState.height = 700;

        renderInitialViewport({enabled: false});

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('fits the flow once it is laid out wider than the canvas', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        flowState.bounds = {height: 400, width: 2000, x: -400, y: 0};
        flowState.height = 700;

        renderInitialViewport();

        expect(fitViewMock).not.toHaveBeenCalled();

        setLaidOutNodes(['trigger_1', 'aiAgent_1']);

        expect(fitViewMock).toHaveBeenCalledTimes(1);
    });

    it('keeps the opening viewport when the laid out flow fits across', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        flowState.bounds = {height: 5000, width: 600, x: 100, y: 0};
        flowState.height = 700;

        renderInitialViewport();

        setLaidOutNodes(['trigger_1', 'aiAgent_1']);

        expect(fitViewMock).not.toHaveBeenCalled();
    });

    it('waits for every top-level task before checking the fit', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        flowState.bounds = {height: 400, width: 2000, x: -400, y: 0};
        flowState.height = 700;

        renderInitialViewport();

        setLaidOutNodes(['trigger_1']);

        expect(fitViewMock).not.toHaveBeenCalled();
    });

    it('checks the fit only once per opened workflow', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        flowState.bounds = {height: 400, width: 600, x: 100, y: 0};
        flowState.height = 700;

        renderInitialViewport();

        setLaidOutNodes(['trigger_1', 'aiAgent_1']);

        flowState.bounds = {height: 400, width: 2000, x: -400, y: 0};

        setLaidOutNodes(['trigger_1', 'aiAgent_1', 'model_1']);

        expect(fitViewMock).not.toHaveBeenCalled();
    });
});
