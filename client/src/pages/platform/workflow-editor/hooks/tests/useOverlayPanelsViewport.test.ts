import useOverlayPanelsViewport, {
    OVERLAY_PAN_DURATION,
} from '@/pages/platform/workflow-editor/hooks/useOverlayPanelsViewport';
import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {DATA_PILL_PANEL_WIDTH, NODE_DETAILS_PANEL_WIDTH, WORKFLOW_NODES_SIDEBAR_WIDTH} from '@/shared/constants';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

interface ViewportI {
    x: number;
    y: number;
    zoom: number;
}

const {setViewportMock, storeState} = vi.hoisted(() => ({
    setViewportMock: vi.fn<(viewport: ViewportI, options?: {duration?: number}) => Promise<boolean>>(() =>
        Promise.resolve(true)
    ),
    storeState: {
        transform: [0, 40, 1] as [number, number, number],
    },
}));

vi.mock('@xyflow/react', () => ({
    useReactFlow: () => ({setViewport: setViewportMock}),
    useStoreApi: () => ({getState: () => storeState}),
}));

const SIDEBAR_OFFSET = -WORKFLOW_NODES_SIDEBAR_WIDTH / 2;
const DETAILS_OFFSET = -NODE_DETAILS_PANEL_WIDTH / 2;
const DATA_PILL_OFFSET = -DATA_PILL_PANEL_WIDTH / 2;

function lastViewport() {
    const calls = setViewportMock.mock.calls;

    return calls[calls.length - 1];
}

function settleAt(x: number) {
    act(() => {
        vi.advanceTimersByTime(OVERLAY_PAN_DURATION);
    });

    storeState.transform = [x, 40, 1];
}

describe('useOverlayPanelsViewport', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        setViewportMock.mockClear();

        storeState.transform = [0, 40, 1];

        useRightSidebarStore.setState({rightSidebarOpen: false});
        useWorkflowIssuesStore.setState({issuesSidebarOpen: false});
        useWorkflowTestChatStore.setState({workflowTestChatPanelOpen: false});
        useDataPillPanelStore.setState({dataPillPanelOpen: false});
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false});
        useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: false});
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('leaves the viewport alone when nothing is open', () => {
        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('applies an overlay that is already open on mount without animating', () => {
        useRightSidebarStore.setState({rightSidebarOpen: true});

        const {result} = renderHook(() => useOverlayPanelsViewport({enabled: true}));

        expect(setViewportMock).toHaveBeenCalledTimes(1);
        expect(lastViewport()).toEqual([{x: SIDEBAR_OFFSET, y: 40, zoom: 1}]);
        expect(result.current.getViewportOffsetX()).toBe(SIDEBAR_OFFSET);
    });

    it('re-centres by half the sidebar width and pans back on close', () => {
        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        act(() => {
            useRightSidebarStore.setState({rightSidebarOpen: true});
        });

        const [viewport, options] = lastViewport();

        expect(viewport).toEqual({x: SIDEBAR_OFFSET, y: 40, zoom: 1});
        expect(options).toMatchObject({duration: OVERLAY_PAN_DURATION});

        settleAt(SIDEBAR_OFFSET);

        act(() => {
            useRightSidebarStore.setState({rightSidebarOpen: false});
        });

        expect(setViewportMock).toHaveBeenCalledTimes(2);
        expect(lastViewport()[0]).toEqual({x: 0, y: 40, zoom: 1});
    });

    it('moves the flow left by half the node details panel width and back on close', () => {
        const {result} = renderHook(() => useOverlayPanelsViewport({enabled: true}));

        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: true});
        });

        expect(lastViewport()[0]).toEqual({x: DETAILS_OFFSET, y: 40, zoom: 1});
        expect(result.current.getViewportOffsetX()).toBe(DETAILS_OFFSET);

        settleAt(DETAILS_OFFSET);

        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: false});
        });

        expect(lastViewport()[0]).toEqual({x: 0, y: 40, zoom: 1});
        expect(result.current.getViewportOffsetX()).toBe(0);
    });

    it('pushes further for the data pill panel and slides back when it closes', () => {
        useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: true});

        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        storeState.transform = [DETAILS_OFFSET, 40, 1];

        act(() => {
            useDataPillPanelStore.setState({dataPillPanelOpen: true});
        });

        expect(lastViewport()[0]).toEqual({x: DETAILS_OFFSET + DATA_PILL_OFFSET, y: 40, zoom: 1});

        settleAt(DETAILS_OFFSET + DATA_PILL_OFFSET);

        act(() => {
            useDataPillPanelStore.setState({dataPillPanelOpen: false});
        });

        expect(lastViewport()[0]).toEqual({x: DETAILS_OFFSET, y: 40, zoom: 1});
    });

    it('keeps a user pan made between toggles', () => {
        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        act(() => {
            useWorkflowIssuesStore.setState({issuesSidebarOpen: true});
        });

        settleAt(SIDEBAR_OFFSET + 500);

        act(() => {
            useWorkflowIssuesStore.setState({issuesSidebarOpen: false});
        });

        expect(lastViewport()[0].x).toBe(500);
    });

    it('combines the sidebar release and the details pan on the issues-to-details handoff', () => {
        useWorkflowIssuesStore.setState({issuesSidebarOpen: true});

        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        storeState.transform = [SIDEBAR_OFFSET, 40, 1];
        setViewportMock.mockClear();

        act(() => {
            useWorkflowIssuesStore.setState({issuesSidebarOpen: false});
            useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: true});
        });

        expect(setViewportMock).toHaveBeenCalledTimes(1);
        expect(lastViewport()[0]).toEqual({x: DETAILS_OFFSET, y: 40, zoom: 1});

        settleAt(DETAILS_OFFSET);

        act(() => {
            useWorkflowNodeDetailsPanelStore.setState({workflowNodeDetailsPanelOpen: false});
        });

        expect(lastViewport()[0]).toEqual({x: 0, y: 40, zoom: 1});
    });

    it('continues from the pending target when a toggle interrupts an animation', () => {
        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        act(() => {
            useRightSidebarStore.setState({rightSidebarOpen: true});
        });

        storeState.transform = [-50, 40, 1];

        act(() => {
            useRightSidebarStore.setState({rightSidebarOpen: false});
        });

        expect(lastViewport()[0]).toEqual({x: 0, y: 40, zoom: 1});
    });

    it('does nothing while disabled', () => {
        useRightSidebarStore.setState({rightSidebarOpen: true});

        renderHook(() => useOverlayPanelsViewport({enabled: false}));

        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('waits while the cluster elements canvas covers the graph and reconciles once it closes', () => {
        useWorkflowEditorStore.setState({clusterElementsCanvasOpen: true});

        renderHook(() => useOverlayPanelsViewport({enabled: true}));

        act(() => {
            useRightSidebarStore.setState({rightSidebarOpen: true});
        });

        expect(setViewportMock).not.toHaveBeenCalled();

        act(() => {
            useWorkflowEditorStore.setState({clusterElementsCanvasOpen: false});
        });

        expect(lastViewport()).toEqual([{x: SIDEBAR_OFFSET, y: 40, zoom: 1}]);
    });
});
