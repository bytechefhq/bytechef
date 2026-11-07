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

interface BuildMemberNodeProps {
    contentPosition: {x: number; y: number};
    data?: Record<string, unknown>;
    name: string;
}

function buildMemberNode({contentPosition, data = {}, name}: BuildMemberNodeProps): Node {
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

function seedDispatcherMemberGraph(): void {
    useWorkflowDataStore.setState({
        nodes: buildGraphNodes([
            buildMemberNode({contentPosition: {x: 24, y: 0}, data: {taskDispatcher: true}, name: 'loop_1'}),
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

        clearAllWorkflowMutations();

        autoPlacedGraphPositionsRef.current = {};

        useWorkflowDataStore.setState({
            edges: [],
            nodes: buildGraphNodes([
                buildMemberNode({contentPosition: {x: 24, y: 0}, name: 'task_1'}),
                buildMemberNode({contentPosition: {x: 200, y: 0}, name: 'task_2'}),
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
        expect(findNode(FRAME_ID).width).toBe(600 + MEMBER_SIZE.width + 600);
    });

    it('persists a dropped dispatcher member at the corner of its subtree rather than at its header', () => {
        useWorkflowDataStore.setState({
            nodes: buildGraphNodes([
                buildMemberNode({contentPosition: {x: 74, y: 0}, data: {taskDispatcher: true}, name: 'loop_1'}),
                {
                    data: {loopData: {index: 0, loopId: 'loop_1'}, workflowNodeName: 'inner_1'},
                    id: 'inner_1',
                    measured: MEMBER_SIZE,
                    parentId: FRAME_ID,
                    position: toFrameChildPosition({x: 24, y: 120}),
                    type: 'workflow',
                },
            ]),
            workflow: {
                definition: buildDefinition(['loop_1']),
                id: 'workflow-1',
                nodeNames: [],
                version: 3,
            },
        });

        const {result} = renderCanvas();

        act(() => {
            result.current.handleNodeDragStop({} as MouseEvent, findNode('loop_1'));
        });

        expect(getSavedMemberPosition('loop_1')).toEqual({x: 24, y: 0});
    });

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
