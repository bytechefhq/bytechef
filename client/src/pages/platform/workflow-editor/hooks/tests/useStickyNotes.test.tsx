import {WorkflowStickyNoteType} from '@/shared/types';
import {renderHook} from '@testing-library/react';
import {Node} from '@xyflow/react';
import {act} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useLayoutDirectionStore from '../../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import {STICKY_NOTE_NODE_TYPE, buildStickyNoteNode} from '../../utils/stickyNoteUtils';
import useStickyNotes from '../useStickyNotes';

const hoisted = vi.hoisted(() => ({
    addStickyNoteMock: vi.fn(),
    updateWorkflowMutation: {mutate: vi.fn()} as {mutate: () => void} | undefined,
}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: hoisted.updateWorkflowMutation}),
}));

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useReactFlow: () => ({screenToFlowPosition: (position: {x: number; y: number}) => position}),
}));

vi.mock('../../utils/stickyNoteUtils', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../../utils/stickyNoteUtils')>();

    return {...actual, addStickyNote: hoisted.addStickyNoteMock};
});

const makeStickyNote = (overrides: Partial<WorkflowStickyNoteType> = {}): WorkflowStickyNoteType => ({
    color: 'yellow',
    content: 'A note',
    id: 'stickyNote_1',
    position: {x: 100, y: 200},
    ...overrides,
});

const makeDefinition = (stickyNotes: Array<WorkflowStickyNoteType>) =>
    JSON.stringify({label: 'Test Workflow', metadata: {ui: {stickyNotes}}, tasks: []});

const taskNode = (): Node => ({data: {}, id: 'task_1', position: {x: 0, y: 0}, type: 'workflow'});

const setWorkflowState = (definition: string | undefined, nodes: Array<Node> = []) =>
    useWorkflowDataStore.setState((state) => ({
        isNodeDragging: false,
        nodes,
        savedPositionCrossAxisShift: 0,
        workflow: {...state.workflow, definition, id: 'workflow_1', version: 1},
    }));

describe('useStickyNotes', () => {
    beforeEach(() => {
        hoisted.addStickyNoteMock.mockClear();
        hoisted.updateWorkflowMutation = {mutate: vi.fn()};

        useLayoutDirectionStore.setState({layoutDirection: 'TB'});

        setWorkflowState(makeDefinition([]));
    });

    it('should add a note at the center of the visible canvas', () => {
        window.innerWidth = 1000;
        window.innerHeight = 800;

        const {result} = renderHook(() => useStickyNotes({readOnly: false}));

        act(() => result.current.handleAddStickyNote());

        expect(hoisted.addStickyNoteMock).toHaveBeenCalledTimes(1);
        expect(hoisted.addStickyNoteMock.mock.calls[0][0].position).toEqual({x: 500 - 240 / 2, y: 400 - 160 / 2});
    });

    it('should spread consecutive notes over a grid instead of stacking them', () => {
        window.innerWidth = 1000;
        window.innerHeight = 800;

        const positions = [];

        for (const noteCount of [0, 1, 8, 9]) {
            const stickyNotes = Array.from({length: noteCount}, (_unused, index) =>
                makeStickyNote({id: `stickyNote_${index}`})
            );

            setWorkflowState(makeDefinition(stickyNotes));

            const {result} = renderHook(() => useStickyNotes({readOnly: false}));

            act(() => result.current.handleAddStickyNote());

            positions.push(hoisted.addStickyNoteMock.mock.calls.at(-1)![0].position);
        }

        expect(positions[1]).toEqual({x: positions[0].x + 24, y: positions[0].y});
        expect(positions[2]).toEqual({x: positions[0].x, y: positions[0].y + 24});
        expect(new Set(positions.map((position) => `${position.x},${position.y}`)).size).toBe(positions.length);
    });

    it('should do nothing without a workflow mutation', () => {
        hoisted.updateWorkflowMutation = undefined;

        const {result} = renderHook(() => useStickyNotes({readOnly: false}));

        act(() => result.current.handleAddStickyNote());

        expect(hoisted.addStickyNoteMock).not.toHaveBeenCalled();
    });

    it('should add the notes of the definition to the canvas, keeping the other nodes', () => {
        setWorkflowState(makeDefinition([makeStickyNote()]), [taskNode()]);

        renderHook(() => useStickyNotes({readOnly: false}));

        const nodes = useWorkflowDataStore.getState().nodes;

        expect(nodes.map((node) => node.id)).toEqual(['task_1', 'stickyNote_1']);
        expect(nodes[1].type).toBe(STICKY_NOTE_NODE_TYPE);
    });

    it('should update a note node whose content changed', () => {
        const staleNode = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: false,
            stickyNote: makeStickyNote({content: 'Old'}),
        });

        setWorkflowState(makeDefinition([makeStickyNote({content: 'New'})]), [staleNode]);

        renderHook(() => useStickyNotes({readOnly: false}));

        expect(useWorkflowDataStore.getState().nodes[0].data).toMatchObject({content: 'New'});
    });

    it('should remove a note node that is no longer in the definition', () => {
        const removedNode = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: false,
            stickyNote: makeStickyNote(),
        });

        setWorkflowState(makeDefinition([]), [taskNode(), removedNode]);

        renderHook(() => useStickyNotes({readOnly: false}));

        expect(useWorkflowDataStore.getState().nodes.map((node) => node.id)).toEqual(['task_1']);
    });

    it('should leave the canvas untouched when nothing changed', () => {
        const stickyNoteNode = buildStickyNoteNode({
            crossAxis: 'x',
            crossAxisShift: 0,
            readOnly: false,
            stickyNote: makeStickyNote(),
        });

        setWorkflowState(makeDefinition([makeStickyNote()]), [stickyNoteNode]);

        const nodesBefore = useWorkflowDataStore.getState().nodes;

        renderHook(() => useStickyNotes({readOnly: false}));

        expect(useWorkflowDataStore.getState().nodes).toBe(nodesBefore);
    });

    it('should not touch the canvas in read-only mode or while a node is dragged', () => {
        setWorkflowState(makeDefinition([makeStickyNote()]), [taskNode()]);

        renderHook(() => useStickyNotes({readOnly: true}));

        expect(useWorkflowDataStore.getState().nodes.map((node) => node.id)).toEqual(['task_1']);

        useWorkflowDataStore.setState({isNodeDragging: true});

        renderHook(() => useStickyNotes({readOnly: false}));

        expect(useWorkflowDataStore.getState().nodes.map((node) => node.id)).toEqual(['task_1']);
    });
});
