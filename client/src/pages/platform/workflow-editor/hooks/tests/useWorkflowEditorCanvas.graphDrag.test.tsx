import {SPACE} from '@/shared/constants';
import {act, render, renderHook} from '@testing-library/react';
import {Node, NodeChange, ReactFlow, ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {
    GRAPH_FRAME_MIN_HEIGHT,
    GRAPH_FRAME_MIN_WIDTH,
    getGraphFrameId,
    toFrameChildPosition,
} from '../../utils/graph/graphFrameGeometry';
import {clearAllWorkflowMutations} from '../../utils/workflowMutationGuard';
import useWorkflowEditorCanvas from '../useWorkflowEditorCanvas';

const {autoPlacedGraphPositionsRef, updateWorkflowMutationMock} = vi.hoisted(() => ({
    autoPlacedGraphPositionsRef: {current: {} as Record<string, Record<string, {x: number; y: number}>>},
    updateWorkflowMutationMock: {isPending: false, mutate: vi.fn()},
}));

vi.mock('../useLayout', () => ({
    default: () => ({autoPlacedGraphPositionsRef}),
}));

vi.mock('../useHandleDrop', () => ({
    default: () => [vi.fn(), vi.fn(), vi.fn(), vi.fn()],
}));

vi.mock('../useStickyNotes', () => ({
    default: () => ({handleAddStickyNote: vi.fn()}),
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: updateWorkflowMutationMock,
    }),
}));

const GRAPH_ID = 'graph_1';
const FRAME_ID = getGraphFrameId(GRAPH_ID);
const MEMBER_SIZE = {height: 72, width: 72};

function buildDefinition(memberNames: string[]): string {
    return JSON.stringify(
        {
            tasks: [
                {
                    name: GRAPH_ID,
                    parameters: {
                        nodes: memberNames.map((memberName) => ({name: memberName, type: 'mailchimp/v1/subscribe'})),
                        transitions: [],
                    },
                    type: 'graph/v1',
                },
            ],
        },
        null,
        SPACE
    );
}

function buildMemberNode(name: string, contentPosition: {x: number; y: number}, data = {}): Node {
    return {
        data: {graphData: {graphId: GRAPH_ID, index: 0}, workflowNodeName: name, ...data},
        draggable: true,
        id: name,
        measured: MEMBER_SIZE,
        parentId: FRAME_ID,
        position: toFrameChildPosition(contentPosition),
        type: 'workflow',
    };
}

function buildGraphNodes(extraNodes: Node[] = []): Node[] {
    return [
        {
            data: {taskDispatcher: true, taskDispatcherId: GRAPH_ID, workflowNodeName: GRAPH_ID},
            id: GRAPH_ID,
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {
            data: {
                graphFrame: {graphId: GRAPH_ID, height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH},
                graphId: GRAPH_ID,
                taskDispatcherId: GRAPH_ID,
            },
            height: GRAPH_FRAME_MIN_HEIGHT,
            id: FRAME_ID,
            position: {x: 0, y: 200},
            type: 'graphFrame',
            width: GRAPH_FRAME_MIN_WIDTH,
        },
        ...extraNodes,
    ];
}

function renderCanvas() {
    return renderHook(() => useWorkflowEditorCanvas({componentDefinitions: [], taskDispatcherDefinitions: []}), {
        wrapper: ({children}: {children: ReactNode}) => <ReactFlowProvider>{children}</ReactFlowProvider>,
    });
}

function positionChange(id: string, position: {x: number; y: number}): NodeChange<Node> {
    return {dragging: true, id, position, type: 'position'};
}

interface SavedMemberTaskI {
    metadata?: {ui?: {nodePosition?: {x: number; y: number}}};
    name: string;
}

/** Reads the graph's members back out of the definition the last position save sent. */
function getSavedMemberPosition(memberName: string): {x: number; y: number} | undefined {
    const [[mutationVariables]] = updateWorkflowMutationMock.mutate.mock.calls.slice(-1);

    const savedTasks: Array<{parameters: {nodes: SavedMemberTaskI[]}}> = JSON.parse(
        (mutationVariables as {workflow: {definition: string}}).workflow.definition
    ).tasks;

    return savedTasks[0].parameters.nodes.find((memberTask) => memberTask.name === memberName)?.metadata?.ui
        ?.nodePosition;
}

function findNode(id: string): Node {
    return useWorkflowDataStore.getState().nodes.find((node) => node.id === id)!;
}

/** A graph whose single member is a `loop` dispatcher carrying one node in its own subtree. */
function seedDispatcherMemberGraph(): void {
    useWorkflowDataStore.setState({
        nodes: buildGraphNodes([
            buildMemberNode('loop_1', {x: 24, y: 0}, {taskDispatcher: true}),
            {
                data: {loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner_1'},
                id: 'inner_1',
                measured: MEMBER_SIZE,
                parentId: FRAME_ID,
                position: toFrameChildPosition({x: 24, y: 120}),
                type: 'workflow',
            },
        ]),
    });
}

describe('useWorkflowEditorCanvas graph member dragging', () => {
    beforeEach(() => {
        updateWorkflowMutationMock.mutate.mockClear();

        // `saveWorkflowNodesPosition` marks the workflow as mutating until the mutation settles,
        // and the mock never does — without this reset every save after the first is queued
        // instead of fired.
        clearAllWorkflowMutations();

        autoPlacedGraphPositionsRef.current = {};

        useWorkflowDataStore.setState({
            edges: [],
            nodes: buildGraphNodes([
                buildMemberNode('task_1', {x: 24, y: 0}),
                buildMemberNode('task_2', {x: 200, y: 0}),
            ]),
            savedPositionCrossAxisShift: 0,
            workflow: {
                definition: buildDefinition(['task_1', 'task_2']),
                id: 'workflow-1',
                nodeNames: [],
                version: 3,
            },
        });
    });

    it('persists a dropped member at its content-origin position', () => {
        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('task_1'),
                position: toFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(getSavedMemberPosition('task_1')).toEqual({x: 220, y: 140});
    });

    it('clamps a member dropped inside the header band to the content origin', () => {
        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {...findNode('task_1'), position: {x: -12, y: 10}});
        });

        expect(getSavedMemberPosition('task_1')).toEqual({x: 0, y: 0});
    });

    it('flushes the graph pending auto-placed sibling positions in the same save', () => {
        autoPlacedGraphPositionsRef.current = {[GRAPH_ID]: {task_2: {x: 400, y: 60}}};

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('task_1'),
                position: toFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(getSavedMemberPosition('task_1')).toEqual({x: 220, y: 140});
        expect(getSavedMemberPosition('task_2')).toEqual({x: 400, y: 60});
        expect(autoPlacedGraphPositionsRef.current[GRAPH_ID]).toBeUndefined();
    });

    it('leaves a member position free of the outer cross-axis compensation', () => {
        useWorkflowDataStore.setState({savedPositionCrossAxisShift: 100});

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('task_1'),
                position: toFrameChildPosition({x: 220, y: 140}),
            });
        });

        expect(getSavedMemberPosition('task_1')).toEqual({x: 220, y: 140});
    });

    it('grows the frame while a member is dragged past its right edge', () => {
        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodesChange([positionChange('task_1', toFrameChildPosition({x: 600, y: 0}))]);
        });

        // task_2 stays at x: 200 and is now the leftmost member, so that inset is mirrored right.
        expect(findNode(FRAME_ID).width).toBe(600 + MEMBER_SIZE.width + 200);
    });

    it('carries a dragged dispatcher member subtree at a constant offset', () => {
        seedDispatcherMemberGraph();

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStart({} as MouseEvent, findNode('loop_1'));
        });

        act(() => {
            result.current.handleNodesChange([positionChange('loop_1', toFrameChildPosition({x: 600, y: 50}))]);
        });

        expect(findNode('inner_1').position).toEqual(toFrameChildPosition({x: 600, y: 170}));
        // loop_1 is the only member, so its own x: 600 inset is what gets mirrored right.
        expect(findNode(FRAME_ID).width).toBe(600 + MEMBER_SIZE.width + 600);
    });

    // A member's subtree is parented to the frame too, but has no position in the graph model —
    // the layout places it from its member's spot. Persisting one would leave a `nodePosition`
    // nothing reads until the task left the frame, when it would be read as an absolute pin.
    it('persists nothing for a member subtree node dragged inside the frame', () => {
        seedDispatcherMemberGraph();

        autoPlacedGraphPositionsRef.current = {[GRAPH_ID]: {loop_1: {x: 400, y: 60}}};

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, {
                ...findNode('inner_1'),
                position: toFrameChildPosition({x: 500, y: 300}),
            });
        });

        expect(updateWorkflowMutationMock.mutate).not.toHaveBeenCalled();
        expect(autoPlacedGraphPositionsRef.current[GRAPH_ID]).toEqual({loop_1: {x: 400, y: 60}});
    });

    it('does not move members a second time when the graph container itself is dragged', () => {
        const {result} = renderCanvas();

        const memberStartPosition = findNode('task_1').position;

        act(() => {
            result.current.handleNodeDragStart({} as MouseEvent, findNode(GRAPH_ID));
        });

        act(() => {
            result.current.handleNodesChange([positionChange(GRAPH_ID, {x: 300, y: 0})]);
        });

        expect(findNode('task_1').position).toEqual(memberStartPosition);
        expect(findNode(FRAME_ID).position).toEqual({x: 300, y: 200});
    });
});

describe('React Flow per-node draggable', () => {
    // Pins the rule Task 6 relies on instead of inverting the canvas-wide drag policy: a node's own
    // `draggable: true` wins over a global `nodesDraggable={false}`, which is what keeps graph
    // members draggable while the canvas is locked. A React Flow upgrade that changes it must fail
    // here rather than silently freeze member dragging.
    it('honours a node draggable flag while the canvas drag lock is on', () => {
        const {container} = render(
            <ReactFlowProvider>
                <ReactFlow
                    nodes={[
                        {data: {}, draggable: true, id: 'member', position: {x: 0, y: 0}},
                        {data: {}, id: 'locked', position: {x: 0, y: 100}},
                    ]}
                    nodesDraggable={false}
                />
            </ReactFlowProvider>
        );

        expect(container.querySelector('[data-id="member"]')!.classList.contains('draggable')).toBe(true);
        expect(container.querySelector('[data-id="locked"]')!.classList.contains('draggable')).toBe(false);
    });
});
