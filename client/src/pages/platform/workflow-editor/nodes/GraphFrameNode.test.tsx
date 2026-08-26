import {NodeDataType} from '@/shared/types';
import {act, render, screen, waitFor} from '@testing-library/react';
import {Node, ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {
    GRAPH_MEMBER_BOX_WIDTH,
    GRAPH_MEMBER_NOMINAL_SIZE,
    getGraphStartPinnedBox,
    toFrameChildPosition,
} from '../utils/graph/graphFrameGeometry';
import GraphFrameNode from './GraphFrameNode';

const {
    autoPlaceGraphMembersMock,
    directionStoreState,
    editorContext,
    recordedPopoverProps,
    saveWorkflowNodesPositionMock,
    updateWorkflowMutationMock,
} = vi.hoisted(() => {
    const mutation = {isPending: false, mutate: vi.fn()};

    return {
        autoPlaceGraphMembersMock: vi.fn(),
        directionStoreState: {layoutDirection: 'TB'},
        editorContext: {updateWorkflowMutation: mutation as typeof mutation | undefined},
        recordedPopoverProps: {value: undefined as Record<string, unknown> | undefined},
        saveWorkflowNodesPositionMock: vi.fn(),
        updateWorkflowMutationMock: mutation,
    };
});

vi.mock('../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => editorContext,
}));

vi.mock('../utils/saveWorkflowNodesPosition', () => ({default: saveWorkflowNodesPositionMock}));

// ELK is loaded lazily and laid out asynchronously; the placement itself has its own coverage, so
// this test only pins what the frame hands it and what it does with the result.
vi.mock('../utils/graph/graphFrameGeometry', async (importOriginal) => ({
    ...(await importOriginal<typeof import('../utils/graph/graphFrameGeometry')>()),
    autoPlaceGraphMembers: autoPlaceGraphMembersMock,
}));

// The component popover is a heavy standalone surface with its own coverage; rendering it as a
// passthrough keeps the Add node trigger itself real while leaving the menu out of this test. Its
// props are recorded so the wiring between the trigger and the insertion machinery can be asserted.
vi.mock('@/pages/platform/workflow-editor/components/WorkflowNodesPopoverMenu', () => ({
    default: ({children, ...popoverProps}: {children: ReactNode}) => {
        recordedPopoverProps.value = popoverProps;

        return <div>{children}</div>;
    },
}));

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector(directionStoreState),
}));

const GRAPH_FRAME_DATA = {
    graphFrame: {graphId: 'graph_1', height: 300, width: 400},
    graphId: 'graph_1',
    taskDispatcherId: 'graph_1',
} as unknown as NodeDataType;

function buildMemberNode(name: string, contentPosition: {x: number; y: number}): Node {
    return {
        data: {graphData: {graphId: 'graph_1', index: 0}},
        id: name,
        parentId: 'graph_1-graph-frame',
        position: toFrameChildPosition(contentPosition),
        type: 'workflow',
    };
}

function renderFrame(data: NodeDataType = GRAPH_FRAME_DATA, id: string = 'graph_1-graph-frame') {
    return render(
        <ReactFlowProvider>
            <GraphFrameNode data={data} id={id} />
        </ReactFlowProvider>
    );
}

describe('GraphFrameNode', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
        editorContext.updateWorkflowMutation = updateWorkflowMutationMock;
        recordedPopoverProps.value = undefined;

        autoPlaceGraphMembersMock.mockReset();
        saveWorkflowNodesPositionMock.mockReset();

        useWorkflowEditorStore.getState().setGraphPendingConnection(undefined);
        useWorkflowDataStore.setState({nodes: []});
    });

    it('sizes the box from the frame data the layout pre-pass computed', () => {
        const {container} = renderFrame();

        const frameBox = container.querySelector('[data-nodetype="graphFrame"]') as HTMLElement;

        expect(frameBox.style.height).toBe('300px');
        expect(frameBox.style.width).toBe('400px');
    });

    it('renders the Auto-arrange and Add node header controls', () => {
        renderFrame();

        expect(screen.getByRole('button', {name: 'Auto-arrange'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add node'})).toBeInTheDocument();
    });

    // An execution view has no mutation to edit the workflow with, and every other affordance on
    // that canvas is already read-only — the task nodes are swapped out, the transitions stamped.
    // Two disabled buttons would be the only editing chrome left on a surface that has none.
    it('leaves the header controls out when there is no mutation to edit the workflow with', () => {
        editorContext.updateWorkflowMutation = undefined;

        renderFrame();

        expect(screen.queryByRole('button', {name: 'Auto-arrange'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add node'})).not.toBeInTheDocument();
        expect(screen.getByText('Graph')).toBeInTheDocument();
    });

    // The hit test that resolves a graph from a pointer release (a transition dropped on empty
    // frame space) and from a component drop walks up to this attribute — there is no other way
    // back from a DOM point to the graph, so losing it silently disables both.
    it('carries its graph id in the attribute the canvas hit-tests', () => {
        const {container} = renderFrame();

        const frameBox = container.querySelector('[data-graph-frame-id]') as HTMLElement;

        expect(frameBox.getAttribute('data-graph-frame-id')).toBe('graph_1');
        expect(frameBox.querySelector('button')?.closest('[data-graph-frame-id]')).toBe(frameBox);
    });

    it('lays every member out and persists the result when Auto-arrange is used', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {parameters: {transitions: [{from: 'task_1', to: 'task_2'}]}, taskDispatcher: true},
                    id: 'graph_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                } as Node,
                buildMemberNode('task_1', {x: 24, y: 0}),
                buildMemberNode('task_2', {x: 300, y: 0}),
            ],
        });

        autoPlaceGraphMembersMock.mockResolvedValue({task_1: {x: 0, y: 0}, task_2: {x: 320, y: 0}});

        renderFrame();

        await act(async () => {
            screen.getByRole('button', {name: 'Auto-arrange'}).click();
        });

        // The PAINTED box, with what the label adds beyond it carried alongside — the same sizing the
        // layout pre-pass auto-places by, so re-arranging a graph by hand cannot lay it out differently
        // from how it was laid out on load.
        const expectedMemberSize = {
            height: GRAPH_MEMBER_NOMINAL_SIZE.height,
            labelOverhang: GRAPH_MEMBER_NOMINAL_SIZE.width - GRAPH_MEMBER_BOX_WIDTH,
            width: GRAPH_MEMBER_BOX_WIDTH,
        };

        expect(autoPlaceGraphMembersMock).toHaveBeenCalledWith(
            [
                {...expectedMemberSize, name: 'task_1'},
                {...expectedMemberSize, name: 'task_2'},
            ],
            [{from: 'task_1', to: 'task_2'}],
            [getGraphStartPinnedBox()],
            'TB',
            'task_1'
        );

        expect(saveWorkflowNodesPositionMock).toHaveBeenCalledWith({
            draggedNodeId: 'graph_1',
            nodePositions: {task_1: {x: 0, y: 0}, task_2: {x: 320, y: 0}},
            updateWorkflowMutation: updateWorkflowMutationMock,
        });
    });

    it('does nothing when Auto-arrange is used on an empty graph', async () => {
        renderFrame();

        await act(async () => {
            screen.getByRole('button', {name: 'Auto-arrange'}).click();
        });

        expect(autoPlaceGraphMembersMock).not.toHaveBeenCalled();
        expect(saveWorkflowNodesPositionMock).not.toHaveBeenCalled();
    });

    it('opens the component popover for a connection dropped inside its own box', async () => {
        renderFrame();

        expect(recordedPopoverProps.value?.open).toBe(false);

        act(() => {
            useWorkflowEditorStore
                .getState()
                .setGraphPendingConnection({dropPosition: {x: 10, y: 20}, from: 'task_1', graphId: 'graph_1'});
        });

        await waitFor(() => expect(recordedPopoverProps.value?.open).toBe(true));
    });

    // A component dropped into the box raises a pending connection purely to carry its position;
    // it already named itself, so opening the picker would leave the component list sitting on top
    // of the node it just created.
    it('leaves the popover shut for a component drop that only carries a position', async () => {
        renderFrame();

        act(() => {
            useWorkflowEditorStore
                .getState()
                .setGraphPendingConnection({dropPosition: {x: 10, y: 20}, from: '', graphId: 'graph_1'});
        });

        await waitFor(() => expect(recordedPopoverProps.value).toBeDefined());

        expect(recordedPopoverProps.value?.open).toBe(false);
    });

    it('leaves the popover shut for a connection dropped inside another graph', async () => {
        renderFrame();

        act(() => {
            useWorkflowEditorStore
                .getState()
                .setGraphPendingConnection({dropPosition: {x: 10, y: 20}, from: 'other_1', graphId: 'graph_2'});
        });

        await waitFor(() => expect(recordedPopoverProps.value).toBeDefined());

        expect(recordedPopoverProps.value?.open).toBe(false);
    });

    it('abandons the pending connection when the popover is dismissed', async () => {
        renderFrame();

        act(() => {
            useWorkflowEditorStore
                .getState()
                .setGraphPendingConnection({dropPosition: {x: 10, y: 20}, from: 'task_1', graphId: 'graph_1'});
        });

        await waitFor(() => expect(recordedPopoverProps.value?.open).toBe(true));

        act(() => {
            (recordedPopoverProps.value?.onOpenChange as (open: boolean) => void)(false);
        });

        expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
    });

    // The placeholder id is the only link between this button and the insertion machinery
    // (`getContextFromPlaceholderNode` resolves the graph from it), so a typo here would leave
    // Add node silently inert.
    it('points the Add node popover at the graph add-node placeholder', () => {
        renderFrame();

        expect(recordedPopoverProps.value?.sourceNodeId).toBe('graph_1-graph-placeholder');
        expect(recordedPopoverProps.value?.hideTriggerComponents).toBe(true);
    });

    it('keeps the header controls out of the canvas drag handler', () => {
        renderFrame();

        expect(screen.getByRole('button', {name: 'Auto-arrange'}).className).toContain('nodrag');
        expect(screen.getByRole('button', {name: 'Add node'}).className).toContain('nodrag');
    });

    it('labels the box so a user can tell what the container is', () => {
        renderFrame();

        expect(screen.getByText('Graph')).toBeInTheDocument();
    });

    it('anchors the surrounding chain at the top and bottom of the box in TB', () => {
        const {container} = renderFrame();

        const topHandle = container.querySelector('[data-handleid="graph_1-graph-frame-top"]');
        const bottomHandle = container.querySelector('[data-handleid="graph_1-graph-frame-bottom"]');

        expect(topHandle?.className).toContain('react-flow__handle-top');
        expect(topHandle?.className).toContain('target');
        expect(bottomHandle?.className).toContain('react-flow__handle-bottom');
        expect(bottomHandle?.className).toContain('source');
    });

    // Member positions are stored and say nothing about direction, so a graph flipped from TB to LR
    // would otherwise keep the shape it was given for the other axis.
    it('re-arranges its members when the layout direction changes', async () => {
        useWorkflowDataStore.setState({
            nodes: [
                {
                    data: {parameters: {transitions: [{from: 'task_1', to: 'task_2'}]}, taskDispatcher: true},
                    id: 'graph_1',
                    position: {x: 0, y: 0},
                    type: 'workflow',
                } as Node,
                buildMemberNode('task_1', {x: 24, y: 0}),
                buildMemberNode('task_2', {x: 300, y: 0}),
            ],
        });

        autoPlaceGraphMembersMock.mockResolvedValue({task_1: {x: 0, y: 0}, task_2: {x: 0, y: 160}});

        const {rerender} = renderFrame();

        expect(autoPlaceGraphMembersMock).not.toHaveBeenCalled();

        directionStoreState.layoutDirection = 'LR';

        await act(async () => {
            // A fresh `data` object: the component is memoised, so re-rendering with the same props
            // would skip it entirely and the new direction would never be read.
            rerender(
                <ReactFlowProvider>
                    <GraphFrameNode data={{...GRAPH_FRAME_DATA}} id="graph_1-graph-frame" />
                </ReactFlowProvider>
            );
        });

        await waitFor(() => {
            expect(autoPlaceGraphMembersMock).toHaveBeenCalled();
        });

        expect(autoPlaceGraphMembersMock.mock.calls[0][3]).toBe('LR');

        await waitFor(() => {
            expect(saveWorkflowNodesPositionMock).toHaveBeenCalled();
        });
    });

    it('moves the chain anchors onto the horizontal axis in LR', () => {
        directionStoreState.layoutDirection = 'LR';

        const {container} = renderFrame();

        expect(container.querySelector('[data-handleid="graph_1-graph-frame-top"]')?.className).toContain(
            'react-flow__handle-left'
        );
        expect(container.querySelector('[data-handleid="graph_1-graph-frame-bottom"]')?.className).toContain(
            'react-flow__handle-right'
        );
    });
});
