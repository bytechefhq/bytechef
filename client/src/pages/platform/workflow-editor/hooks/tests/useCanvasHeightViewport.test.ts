import useCanvasHeightViewport from '@/pages/platform/workflow-editor/hooks/useCanvasHeightViewport';
import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

interface ViewportI {
    x: number;
    y: number;
    zoom: number;
}

const {setViewportMock, storeState} = vi.hoisted(() => ({
    setViewportMock: vi.fn<(viewport: ViewportI) => Promise<boolean>>(() => Promise.resolve(true)),
    storeState: {
        height: 0,
        transform: [10, 40, 1] as [number, number, number],
    },
}));

vi.mock('@xyflow/react', () => ({
    useReactFlow: () => ({setViewport: setViewportMock}),
    useStore: (selector: (state: typeof storeState) => unknown) => selector(storeState),
    useStoreApi: () => ({getState: () => storeState}),
}));

describe('useCanvasHeightViewport', () => {
    beforeEach(() => {
        setViewportMock.mockClear();

        storeState.height = 800;
        storeState.transform = [10, 40, 1];

        useLayoutDirectionStore.setState({layoutDirection: 'LR'});
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false});
    });

    it('leaves the viewport alone on the first measured height', () => {
        renderHook(() => useCanvasHeightViewport({enabled: true}));

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('moves the flow up by half the height the canvas loses', () => {
        const {rerender} = renderHook(() => useCanvasHeightViewport({enabled: true}));

        storeState.height = 500;

        rerender();

        expect(setViewportMock).toHaveBeenLastCalledWith({x: 10, y: -110, zoom: 1});
    });

    it('moves the flow back down when the canvas grows again', () => {
        const {rerender} = renderHook(() => useCanvasHeightViewport({enabled: true}));

        storeState.height = 500;

        rerender();

        storeState.transform = [10, -110, 1];
        storeState.height = 800;

        rerender();

        expect(setViewportMock).toHaveBeenLastCalledWith({x: 10, y: 40, zoom: 1});
    });

    it('ignores a height of zero while the canvas is hidden', () => {
        const {rerender} = renderHook(() => useCanvasHeightViewport({enabled: true}));

        storeState.height = 0;

        rerender();

        storeState.height = 800;

        rerender();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('keeps the top of a top-to-bottom workflow in place', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        const {rerender} = renderHook(() => useCanvasHeightViewport({enabled: true}));

        storeState.height = 450;

        rerender();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('does nothing when disabled', () => {
        const {rerender} = renderHook(() => useCanvasHeightViewport({enabled: false}));

        storeState.height = 500;

        rerender();

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('does nothing while the cluster elements canvas is open', () => {
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});

        const {rerender} = renderHook(() => useCanvasHeightViewport({enabled: true}));

        storeState.height = 500;

        rerender();

        expect(setViewportMock).not.toHaveBeenCalled();
    });
});
