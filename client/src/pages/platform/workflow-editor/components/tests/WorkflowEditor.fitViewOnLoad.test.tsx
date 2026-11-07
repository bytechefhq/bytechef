import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {getAxisCenteredViewport} from '@/pages/platform/workflow-editor/utils/axisCenteredViewportUtils';
import {Workflow} from '@/shared/middleware/platform/configuration';
import {render} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowEditor from '../WorkflowEditor';

const {fitViewMock, flowState, getNodesBoundsMock, setViewportMock} = vi.hoisted(() => ({
    fitViewMock: vi.fn(),
    flowState: {height: 800, nodesInitialized: true, width: 1000},
    getNodesBoundsMock: vi.fn(),
    setViewportMock: vi.fn(),
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    Background: () => null,
    BackgroundVariant: {Dots: 'dots'},
    ReactFlow: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useNodesInitialized: () => flowState.nodesInitialized,
    useReactFlow: () => ({fitView: fitViewMock, getNodesBounds: getNodesBoundsMock, setViewport: setViewportMock}),
    useStore: (selector: (state: {height: number; width: number}) => number) =>
        selector({height: flowState.height, width: flowState.width}),
}));

vi.mock('../../hooks/useWorkflowEditorCanvas', () => ({
    default: () => ({
        edgeTypes: {},
        handleAddStickyNote: vi.fn(),
        handleConnect: vi.fn(),
        handleConnectEnd: vi.fn(),
        handleNodeDragStart: vi.fn(),
        handleNodeDragStop: vi.fn(),
        handleNodesChange: vi.fn(),
        handleReconnect: vi.fn(),
        handleTransitionDeleteKeyDown: vi.fn(),
        isValidConnection: vi.fn(),
        nodeTypes: {},
        onDragOver: vi.fn(),
        onDrop: vi.fn(),
    }),
}));

vi.mock('../../edges/GraphConnectionLine', () => ({default: () => null}));

vi.mock('@/pages/platform/workflow-editor/components/properties/graph/GraphTransitionEditorLayer', () => ({
    default: () => null,
}));

vi.mock('../WorkflowEditorToolbar', () => ({default: () => null}));

vi.mock('../WorkflowIssuesNote', () => ({default: () => null}));

vi.mock('../NodeActionsHint', () => ({default: () => null}));

const BOUNDS = {height: 300, width: 500, x: 250, y: 50};

const NODES = [
    {data: {}, id: 'trigger_1', position: {x: 364, y: 50}},
    {data: {}, id: 'aiAgent_1', position: {x: 250, y: 200}},
] as Node[];

const renderEditor = (props: {fitViewOnLoad?: boolean; onFitView?: () => void} = {}) =>
    render(
        <WorkflowEditor
            componentDefinitions={[]}
            fitViewOnLoad={props.fitViewOnLoad ?? true}
            onFitView={props.onFitView}
            readOnlyWorkflow={{id: 'workflow1'} as Workflow}
            taskDispatcherDefinitions={[]}
        />
    );

describe('WorkflowEditor fit view on load', () => {
    beforeEach(() => {
        fitViewMock.mockReset();
        getNodesBoundsMock.mockReset();
        setViewportMock.mockReset();

        getNodesBoundsMock.mockReturnValue(BOUNDS);

        flowState.height = 800;
        flowState.nodesInitialized = true;
        flowState.width = 1000;

        useLayoutDirectionStore.setState({layoutDirection: 'TB'});
        useWorkflowDataStore.setState({edges: [], nodes: NODES});
    });

    it('centers the trigger icon column in a top-to-bottom layout instead of the label-skewed node bounds', () => {
        const onFitView = vi.fn();

        renderEditor({onFitView});

        const axisX = 364 + 72 / 2;

        const expectedViewport = getAxisCenteredViewport({
            axisX,
            bounds: BOUNDS,
            flowHeight: 800,
            flowWidth: 1000,
            maxZoom: 1,
            minZoom: 0.1,
            padding: 0.15,
        });

        expect(getNodesBoundsMock).toHaveBeenCalledWith(NODES);
        expect(setViewportMock).toHaveBeenCalledWith(expectedViewport, {duration: 0});

        const [viewport] = setViewportMock.mock.lastCall!;

        expect(viewport.x + axisX * viewport.zoom).toBeCloseTo(1000 / 2);
        expect(fitViewMock).not.toHaveBeenCalled();
        expect(onFitView).toHaveBeenCalled();
    });

    it('falls back to fitView in a left-to-right layout', () => {
        useLayoutDirectionStore.setState({layoutDirection: 'LR'});

        renderEditor();

        expect(fitViewMock).toHaveBeenCalledWith({duration: 0, maxZoom: 1, minZoom: 0.1, padding: 0.15});
        expect(setViewportMock).not.toHaveBeenCalled();
    });

    it('waits until the flow has a size', () => {
        flowState.width = 0;

        renderEditor();

        expect(setViewportMock).not.toHaveBeenCalled();
        expect(fitViewMock).not.toHaveBeenCalled();
    });

    it('waits until the nodes are initialized', () => {
        flowState.nodesInitialized = false;

        renderEditor();

        expect(setViewportMock).not.toHaveBeenCalled();
        expect(fitViewMock).not.toHaveBeenCalled();
    });

    it('does not touch the viewport when fitting on load is off', () => {
        renderEditor({fitViewOnLoad: false});

        expect(setViewportMock).not.toHaveBeenCalled();
        expect(fitViewMock).not.toHaveBeenCalled();
    });

    it('re-centers when the flow is resized after the first fit', () => {
        const {rerender} = renderEditor();

        expect(setViewportMock).toHaveBeenCalledTimes(1);

        flowState.width = 1400;

        rerender(
            <WorkflowEditor
                componentDefinitions={[]}
                fitViewOnLoad
                readOnlyWorkflow={{id: 'workflow1'} as Workflow}
                taskDispatcherDefinitions={[]}
            />
        );

        expect(setViewportMock).toHaveBeenCalledTimes(2);

        const [viewport] = setViewportMock.mock.lastCall!;

        expect(viewport.x + (364 + 72 / 2) * viewport.zoom).toBeCloseTo(1400 / 2);
    });
});
