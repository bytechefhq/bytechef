import {GRAPH_START_EDGE_TYPE, GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {act, fireEvent, render, renderHook, waitFor} from '@testing-library/react';
import {
    ConnectionMode,
    FinalConnectionState,
    InternalNode,
    Node,
    ReactFlow,
    ReactFlowProvider,
    useUpdateNodeInternals,
} from '@xyflow/react';
import {ReactNode} from 'react';
import {afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import GraphConnectionLine, {getGraphConnectionDragGraphId} from '../../edges/GraphConnectionLine';
import GraphTransitionHandles from '../../nodes/GraphTransitionHandles';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../../stores/useWorkflowEditorStore';
import {registerAutoPlacedGraphPositions} from '../../utils/graph/autoPlacedGraphPositions';
import {
    GRAPH_FRAME_HEADER_HEIGHT,
    GRAPH_FRAME_ID_ATTRIBUTE,
    getGraphFrameId,
    toFrameChildPosition,
} from '../../utils/graph/graphFrameGeometry';
import useGraphConnections from '../useGraphConnections';

const {saveGraphParametersMock, updateWorkflowMutationMock} = vi.hoisted(() => ({
    saveGraphParametersMock: vi.fn(),
    updateWorkflowMutationMock: {isPending: false, mutate: vi.fn()},
}));

vi.mock('../../utils/graph/saveGraphParameters', () => ({saveGraphParameters: saveGraphParametersMock}));

const GRAPH_ID = 'graph_1';
const FRAME_ID = getGraphFrameId(GRAPH_ID);
const START_ID = `${GRAPH_ID}-graph-start`;
const MEMBER_SIZE = {height: 72, width: 72};

const autoPlacedGraphPositionsRef = {current: {} as Record<string, Record<string, {x: number; y: number}>>};

let unregisterAutoPlacedGraphPositions: (() => void) | undefined;

function buildMemberNode(name: string, contentPosition: {x: number; y: number}): Node {
    return {
        data: {graphData: {graphId: GRAPH_ID, index: 0}},
        id: name,
        measured: MEMBER_SIZE,
        parentId: FRAME_ID,
        position: toFrameChildPosition(contentPosition),
        type: 'member',
    };
}

function buildNodes(transitions: Array<{from: string; to: string}> = []): Node[] {
    return [
        {
            data: {parameters: {nodes: [], transitions}, taskDispatcher: true},
            id: GRAPH_ID,
            position: {x: 0, y: 0},
            type: 'default',
        },
        {
            data: {graphFrame: {graphId: GRAPH_ID, height: 300, width: 400}, graphId: GRAPH_ID},
            height: 300,
            id: FRAME_ID,
            position: {x: 100, y: 100},
            type: 'frame',
            width: 400,
        },
        {
            data: {graphStart: {graphId: GRAPH_ID}},
            id: START_ID,
            parentId: FRAME_ID,
            position: toFrameChildPosition({x: 24, y: 0}),
            type: 'default',
        },
        buildMemberNode('task_1', {x: 24, y: 60}),
        buildMemberNode('task_2', {x: 300, y: 60}),
    ];
}

const MemberNode = ({id}: {id: string}) => (
    <div style={{height: MEMBER_SIZE.height, width: MEMBER_SIZE.width}}>
        <GraphTransitionHandles connectable direction="TB" nodeId={id} />
    </div>
);

const FrameNode = ({data}: {data: {graphId: string}}) => (
    <div {...{[GRAPH_FRAME_ID_ATTRIBUTE]: data.graphId}} style={{height: 300, width: 400}}>
        <span>frame content</span>
    </div>
);

const NODE_TYPES = {frame: FrameNode, member: MemberNode};

/** Applies the updater `saveGraphParameters` was last handed, to inspect the payload it builds. */
function applyLastSavedUpdater(parameters: Record<string, unknown>): Record<string, unknown> {
    const [graphId, updater] = saveGraphParametersMock.mock.calls.at(-1)!;

    expect(graphId).toBe(GRAPH_ID);

    return (updater as (currentParameters: Record<string, unknown>) => Record<string, unknown>)(parameters);
}

function memberConnection(source: string, target: string) {
    return {
        source,
        sourceHandle: `${source}-graph-transition-source`,
        target,
        targetHandle: `${target}-graph-transition-target`,
    };
}

function renderGraphConnections(nodes: Node[]) {
    useWorkflowDataStore.setState({nodes});

    return renderHook(
        () =>
            useGraphConnections({
                updateWorkflowMutation: updateWorkflowMutationMock as unknown as UpdateWorkflowMutationType,
            }),
        {
            wrapper: ({children}: {children: ReactNode}) => (
                <ReactFlowProvider>
                    <ReactFlow nodeTypes={NODE_TYPES} nodes={nodes} />

                    {children}
                </ReactFlowProvider>
            ),
        }
    );
}

describe('useGraphConnections', () => {
    beforeEach(() => {
        saveGraphParametersMock.mockClear();

        autoPlacedGraphPositionsRef.current = {};

        unregisterAutoPlacedGraphPositions = registerAutoPlacedGraphPositions(autoPlacedGraphPositionsRef);

        useWorkflowEditorStore.getState().setGraphPendingConnection(undefined);
    });

    afterEach(() => {
        unregisterAutoPlacedGraphPositions?.();
    });

    it('appends the transition a member-to-member connection draws', () => {
        const {result} = renderGraphConnections(buildNodes());

        act(() => {
            result.current.handleConnect(memberConnection('task_1', 'task_2'));
        });

        expect(applyLastSavedUpdater({nodes: [], transitions: [{from: 'task_2', to: 'task_1'}]})).toEqual({
            nodes: [],
            transitions: [
                {from: 'task_2', to: 'task_1'},
                {from: 'task_1', to: 'task_2'},
            ],
        });
    });

    it('flushes the graph pending auto-placed positions with the transition', () => {
        autoPlacedGraphPositionsRef.current = {[GRAPH_ID]: {task_2: {x: 400, y: 60}}};

        const {result} = renderGraphConnections(buildNodes());

        act(() => {
            result.current.handleConnect(memberConnection('task_1', 'task_2'));
        });

        expect(
            applyLastSavedUpdater({
                nodes: [{name: 'task_1'}, {name: 'task_2', type: 'mailchimp/v1/subscribe'}],
                transitions: [],
            })
        ).toEqual({
            nodes: [
                {name: 'task_1'},
                {metadata: {ui: {nodePosition: {x: 400, y: 60}}}, name: 'task_2', type: 'mailchimp/v1/subscribe'},
            ],
            transitions: [{from: 'task_1', to: 'task_2'}],
        });

        expect(autoPlacedGraphPositionsRef.current[GRAPH_ID]).toBeUndefined();
    });

    it('saves nothing for a connection that is not a graph edit', () => {
        const {result} = renderGraphConnections(buildNodes([{from: 'task_1', to: 'task_2'}]));

        act(() => {
            result.current.handleConnect(memberConnection('task_1', 'task_2'));
        });

        expect(saveGraphParametersMock).not.toHaveBeenCalled();
    });

    it('sets the start node when the start edge is re-pointed', () => {
        const {result} = renderGraphConnections(buildNodes());

        act(() => {
            result.current.handleReconnect(
                {id: `${START_ID}=>task_1`, source: START_ID, target: 'task_1', type: GRAPH_START_EDGE_TYPE},
                {
                    source: START_ID,
                    sourceHandle: `${START_ID}-source`,
                    target: 'task_2',
                    targetHandle: 'task_2-graph-transition-target',
                }
            );
        });

        expect(applyLastSavedUpdater({startNode: 'task_1', transitions: []})).toEqual({
            startNode: 'task_2',
            transitions: [],
        });
    });

    // The start edge only opts its target end into reconnecting, so this shape is unreachable
    // today; a change that let the pill end move must not silently author a transition instead.
    it('ignores a start edge reconnect that moved the pill end rather than the target', () => {
        const {result} = renderGraphConnections(buildNodes());

        act(() => {
            result.current.handleReconnect(
                {id: `${START_ID}=>task_1`, source: START_ID, target: 'task_1', type: GRAPH_START_EDGE_TYPE},
                memberConnection('task_2', 'task_1')
            );
        });

        expect(saveGraphParametersMock).not.toHaveBeenCalled();
    });

    it('ignores a reconnect of anything but the start edge', () => {
        const {result} = renderGraphConnections(buildNodes());

        act(() => {
            result.current.handleReconnect(
                {id: 'graph_1-transition-0', source: 'task_1', target: 'task_2', type: GRAPH_TRANSITION_EDGE_TYPE},
                memberConnection('task_1', 'task_2')
            );
        });

        expect(saveGraphParametersMock).not.toHaveBeenCalled();
    });

    it('accepts a self-loop but rejects a handle that is not a transition endpoint', () => {
        const {result} = renderGraphConnections(buildNodes());

        expect(result.current.isValidConnection(memberConnection('task_1', 'task_1'))).toBe(true);
        expect(
            result.current.isValidConnection({
                source: 'task_1',
                sourceHandle: 'task_1-graph-transition-source',
                target: 'task_2',
                targetHandle: 'task_2-graph-transition-dynamic',
            })
        ).toBe(false);
    });

    describe('handleConnectEnd', () => {
        const originalElementFromPoint = document.elementFromPoint;

        afterEach(() => {
            document.elementFromPoint = originalElementFromPoint;
        });

        function stubPointerTarget(element: Element | null) {
            (document as unknown as {elementFromPoint: () => Element | null}).elementFromPoint = () => element;
        }

        function buildConnectionState(
            fromNode: Node | undefined,
            isValid: boolean,
            fromHandleType: 'source' | 'target' = 'source'
        ): FinalConnectionState {
            return {
                fromHandle: {id: `${fromNode?.id}-graph-transition-${fromHandleType}`, type: fromHandleType},
                fromNode: fromNode as unknown as InternalNode,
                fromPosition: null,
                isValid,
                toHandle: null,
                toNode: null,
                toPosition: null,
            } as unknown as FinalConnectionState;
        }

        it('raises a pending connection for a release over its own frame', () => {
            const nodes = buildNodes();

            const {container} = render(
                <ReactFlowProvider>
                    <ReactFlow nodeTypes={NODE_TYPES} nodes={nodes} />
                </ReactFlowProvider>
            );

            // The hit test walks up from whatever the pointer landed on, so the stub returns a
            // descendant of the frame element rather than the frame itself.
            stubPointerTarget(container.querySelector(`[${GRAPH_FRAME_ID_ATTRIBUTE}] span`));

            const {result} = renderGraphConnections(nodes);

            act(() => {
                result.current.handleConnectEnd(
                    new MouseEvent('mouseup', {clientX: 320, clientY: 260}),
                    buildConnectionState(nodes[3], false)
                );
            });

            expect(useWorkflowEditorStore.getState().graphPendingConnection).toEqual({
                dropPosition: {x: 220, y: 160 - GRAPH_FRAME_HEADER_HEIGHT},
                from: 'task_1',
                graphId: GRAPH_ID,
            });
        });

        it('raises nothing for a release outside any frame', () => {
            const nodes = buildNodes();

            render(
                <ReactFlowProvider>
                    <ReactFlow nodeTypes={NODE_TYPES} nodes={nodes} />
                </ReactFlowProvider>
            );

            stubPointerTarget(document.body);

            const {result} = renderGraphConnections(nodes);

            act(() => {
                result.current.handleConnectEnd(
                    new MouseEvent('mouseup', {clientX: 900, clientY: 900}),
                    buildConnectionState(nodes[3], false)
                );
            });

            expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
        });

        // A transition drawn backwards would be saved leaving the member the pointer ENDED on.
        it('raises nothing for a drag that began at a target handle', () => {
            const nodes = buildNodes();

            const {container} = render(
                <ReactFlowProvider>
                    <ReactFlow nodeTypes={NODE_TYPES} nodes={nodes} />
                </ReactFlowProvider>
            );

            stubPointerTarget(container.querySelector(`[${GRAPH_FRAME_ID_ATTRIBUTE}] span`));

            const {result} = renderGraphConnections(nodes);

            act(() => {
                result.current.handleConnectEnd(
                    new MouseEvent('mouseup', {clientX: 320, clientY: 260}),
                    buildConnectionState(nodes[3], false, 'target')
                );
            });

            expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
        });

        it('raises nothing when the release already made a connection', () => {
            const nodes = buildNodes();

            const {container} = render(
                <ReactFlowProvider>
                    <ReactFlow nodeTypes={NODE_TYPES} nodes={nodes} />
                </ReactFlowProvider>
            );

            stubPointerTarget(container.querySelector(`[${GRAPH_FRAME_ID_ATTRIBUTE}] span`));

            const {result} = renderGraphConnections(nodes);

            act(() => {
                result.current.handleConnectEnd(
                    new MouseEvent('mouseup', {clientX: 320, clientY: 260}),
                    buildConnectionState(nodes[3], true)
                );
            });

            expect(useWorkflowEditorStore.getState().graphPendingConnection).toBeUndefined();
        });
    });
});

/**
 * Pins the React Flow behaviour the whole graph canvas rests on: `nodesConnectable={false}` is a
 * DEFAULT for nodes that express no opinion, and a `Handle` given its own `isConnectable` is
 * consulted directly — so a member's transition handles stay connectable while every other node on
 * the canvas cannot be connected at all. A React Flow upgrade that routes handle interaction
 * through the canvas-wide flag must fail here rather than silently kill graph editing.
 */
describe('React Flow connections under a canvas-wide nodesConnectable=false', () => {
    const originalGetBoundingClientRect = Element.prototype.getBoundingClientRect;
    const originalElementFromPoint = document.elementFromPoint;

    let updateNodeInternals: ReturnType<typeof useUpdateNodeInternals> | undefined;

    const InternalsProbe = () => {
        updateNodeInternals = useUpdateNodeInternals();

        return null;
    };

    beforeAll(() => {
        // jsdom lays nothing out, and React Flow refuses to register a node's handle bounds while
        // the node measures zero — so the connection could never start for reasons unrelated to
        // the flag under test. A fixed box for every element is enough to get past that.
        Object.defineProperty(window, 'DOMMatrixReadOnly', {
            configurable: true,
            value: class {
                m22 = 1;
            },
            writable: true,
        });

        Object.defineProperty(HTMLElement.prototype, 'offsetWidth', {configurable: true, get: () => MEMBER_SIZE.width});
        Object.defineProperty(HTMLElement.prototype, 'offsetHeight', {
            configurable: true,
            get: () => MEMBER_SIZE.height,
        });

        Element.prototype.getBoundingClientRect = () =>
            ({
                bottom: MEMBER_SIZE.height,
                height: MEMBER_SIZE.height,
                left: 0,
                right: MEMBER_SIZE.width,
                top: 0,
                width: MEMBER_SIZE.width,
                x: 0,
                y: 0,
            }) as DOMRect;
    });

    afterAll(() => {
        Element.prototype.getBoundingClientRect = originalGetBoundingClientRect;
        document.elementFromPoint = originalElementFromPoint;
    });

    function renderConnectableCanvas() {
        const onConnect = vi.fn();
        const onConnectStart = vi.fn();

        const {container} = render(
            <ReactFlowProvider>
                <ReactFlow
                    connectionMode={ConnectionMode.Strict}
                    nodeTypes={NODE_TYPES}
                    nodes={[buildMemberNode('task_1', {x: 0, y: 0}), buildMemberNode('task_2', {x: 200, y: 0})]}
                    nodesConnectable={false}
                    onConnect={onConnect}
                    onConnectStart={onConnectStart}
                >
                    <GraphConnectionLine />

                    <InternalsProbe />
                </ReactFlow>
            </ReactFlowProvider>
        );

        return {container, onConnect, onConnectStart};
    }

    async function measureNodes() {
        await act(async () => {
            updateNodeInternals!(['task_1', 'task_2']);

            await new Promise((resolve) => setTimeout(resolve, 40));
        });
    }

    it('starts and completes a transition drag between two member handles', async () => {
        const {container, onConnect, onConnectStart} = renderConnectableCanvas();

        await measureNodes();

        const sourceHandle = container.querySelector('[data-handleid="task_1-graph-transition-source"]')!;
        const targetHandle = container.querySelector('[data-handleid="task_2-graph-transition-target"]')!;

        expect(sourceHandle.classList.contains('connectable')).toBe(true);

        // React Flow prefers the handle under the pointer, which jsdom cannot resolve on its own.
        (document as unknown as {elementFromPoint: () => Element}).elementFromPoint = () => targetHandle;

        fireEvent.mouseDown(sourceHandle, {button: 0, clientX: 10, clientY: 10});
        fireEvent.mouseMove(document, {clientX: 236, clientY: 36});

        await waitFor(() => expect(onConnectStart).toHaveBeenCalled());

        fireEvent.mouseUp(document, {clientX: 236, clientY: 36});

        expect(onConnect).toHaveBeenCalledWith({
            source: 'task_1',
            sourceHandle: 'task_1-graph-transition-source',
            target: 'task_2',
            targetHandle: 'task_2-graph-transition-target',
        });
    });

    // A transition drawn backwards would be saved leaving the member the pointer ENDED on, so a
    // target handle must refuse to begin a drag at all. React Flow defaults `isConnectableStart`
    // to true and consults it ALONE for pointer-down — `isConnectable` does not gate it — so this
    // holds only while `GraphTransitionHandles` passes the prop.
    it('refuses to start a drag from a member target handle', async () => {
        const {container, onConnectStart} = renderConnectableCanvas();

        await measureNodes();

        const targetHandle = container.querySelector('[data-handleid="task_1-graph-transition-target"]')!;
        const dynamicHandle = container.querySelector('[data-handleid="task_1-graph-transition-dynamic"]')!;

        expect(targetHandle.classList.contains('connectablestart')).toBe(false);
        expect(dynamicHandle.classList.contains('connectablestart')).toBe(false);

        (document as unknown as {elementFromPoint: () => Element | null}).elementFromPoint = () => null;

        fireEvent.mouseDown(targetHandle, {button: 0, clientX: 10, clientY: 10});
        fireEvent.mouseMove(document, {clientX: 160, clientY: 80});

        await act(async () => {
            await new Promise((resolve) => setTimeout(resolve, 20));
        });

        expect(onConnectStart).not.toHaveBeenCalled();
        expect(container.querySelector('[data-graph-connection-line]')).toBeNull();

        fireEvent.mouseUp(document, {clientX: 160, clientY: 80});
    });

    // React Flow's own connection line is suppressed by the very flag that keeps every other node
    // unconnectable, which is the whole reason `GraphConnectionLine` exists. Both halves are
    // asserted together: if an upgrade ever un-suppresses the built-in one, this fails loudly
    // rather than leaving two lines drawn over each other.
    it('draws its own connection line, because React Flow draws none', async () => {
        const {container, onConnectStart} = renderConnectableCanvas();

        await measureNodes();

        expect(container.querySelector('[data-graph-connection-line]')).toBeNull();

        const sourceHandle = container.querySelector('[data-handleid="task_1-graph-transition-source"]')!;

        (document as unknown as {elementFromPoint: () => Element | null}).elementFromPoint = () => null;

        fireEvent.mouseDown(sourceHandle, {button: 0, clientX: 10, clientY: 10});
        fireEvent.mouseMove(document, {clientX: 160, clientY: 80});

        await waitFor(() => expect(onConnectStart).toHaveBeenCalled());

        const overlayPath = container.querySelector('[data-graph-connection-line] path')!;

        expect(overlayPath).not.toBeNull();
        expect(overlayPath.getAttribute('d')).toMatch(/^M/);
        expect(container.querySelector('.react-flow__connectionline')).toBeNull();

        fireEvent.mouseUp(document, {clientX: 160, clientY: 80});

        await waitFor(() => expect(container.querySelector('[data-graph-connection-line]')).toBeNull());
    });
});

describe('getGraphConnectionDragGraphId', () => {
    it('names the graph for a member and for the Start pill, and nothing for anything else', () => {
        expect(getGraphConnectionDragGraphId(buildMemberNode('task_1', {x: 0, y: 0}))).toBe(GRAPH_ID);
        expect(
            getGraphConnectionDragGraphId({
                data: {graphStart: {graphId: GRAPH_ID}},
                id: START_ID,
                position: {x: 0, y: 0},
            })
        ).toBe(GRAPH_ID);
        expect(getGraphConnectionDragGraphId({data: {}, id: 'plain_1', position: {x: 0, y: 0}})).toBeUndefined();
        expect(getGraphConnectionDragGraphId(null)).toBeUndefined();
    });
});
