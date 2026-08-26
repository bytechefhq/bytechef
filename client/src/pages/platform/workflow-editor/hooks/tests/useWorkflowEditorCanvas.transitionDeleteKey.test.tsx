import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {fireEvent, render} from '@testing-library/react';
import {Edge, ReactFlowProvider} from '@xyflow/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowEditor from '../../components/WorkflowEditor';
import useWorkflowDataStore from '../../stores/useWorkflowDataStore';

const {autoPlacedGraphPositionsRef, saveGraphTransitionsMock, updateWorkflowMutationMock} = vi.hoisted(() => ({
    autoPlacedGraphPositionsRef: {current: {} as Record<string, Record<string, {x: number; y: number}>>},
    saveGraphTransitionsMock: vi.fn(),
    updateWorkflowMutationMock: {isPending: false, mutate: vi.fn()},
}));

vi.mock('../useLayout', () => ({default: () => ({autoPlacedGraphPositionsRef})}));

vi.mock('../useHandleDrop', () => ({default: () => [vi.fn(), vi.fn(), vi.fn(), vi.fn(), vi.fn()]}));

vi.mock('../useStickyNotes', () => ({default: () => ({handleAddStickyNote: vi.fn()})}));

vi.mock('../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: updateWorkflowMutationMock,
    }),
}));

vi.mock('../../utils/graph/saveGraphParameters', () => ({saveGraphTransitions: saveGraphTransitionsMock}));

vi.mock('../../components/WorkflowEditorToolbar', () => ({default: () => <div />}));

vi.mock('../../components/NodeActionsHint', () => ({default: () => <div />}));

vi.mock('../../edges/GraphConnectionLine', () => ({default: () => null}));

const GRAPH_ID = 'graph_1';

function buildTransitionEdge(overrides: Partial<Edge> = {}): Edge {
    return {
        data: {graphId: GRAPH_ID, index: 1},
        id: `${GRAPH_ID}-transition-1`,
        selected: true,
        source: 'task_2',
        target: 'task_3',
        type: GRAPH_TRANSITION_EDGE_TYPE,
        ...overrides,
    };
}

function renderEditor(edges: Edge[], readOnly = false) {
    useWorkflowDataStore.setState({
        edges,
        nodes: [],
        workflow: {id: 'workflow-1', nodeNames: [], version: 1},
    });

    const wrapper = ({children}: {children: ReactNode}) => <ReactFlowProvider>{children}</ReactFlowProvider>;

    return render(
        <WorkflowEditor
            componentDefinitions={[]}
            readOnlyWorkflow={readOnly ? {id: 'workflow-1'} : undefined}
            taskDispatcherDefinitions={[]}
        />,
        {wrapper}
    );
}

/** Applies the mutation `saveGraphTransitions` was last handed, to inspect the list it builds. */
function applyLastSavedMutation(
    transitions: Array<{condition?: string; from: string; to: string}>
): Array<{condition?: string; from: string; to: string}> {
    const [graphId, mutate] = saveGraphTransitionsMock.mock.calls.at(-1)!;

    expect(graphId).toBe(GRAPH_ID);

    return (
        mutate as (
            currentTransitions: Array<{condition?: string; from: string; to: string}>
        ) => Array<{condition?: string; from: string; to: string}>
    )(transitions);
}

/** Focuses a detached element of the given markup, returning a cleanup that removes it. */
function focusMarkup(html: string): () => void {
    const host = document.createElement('div');

    host.innerHTML = html;
    document.body.appendChild(host);

    host.querySelector<HTMLElement>('[data-focus]')!.focus();

    return () => host.remove();
}

describe('graph transition delete key', () => {
    beforeEach(() => {
        saveGraphTransitionsMock.mockClear();
    });

    it.each(['Backspace', 'Delete'])('removes the selected transition on %s', (key) => {
        renderEditor([buildTransitionEdge()]);

        fireEvent.keyDown(document, {key});

        expect(
            applyLastSavedMutation([
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_3'},
            ])
        ).toEqual([{from: 'task_1', to: 'task_2'}]);
    });

    it('ignores a key that is not a delete key', () => {
        renderEditor([buildTransitionEdge()]);

        fireEvent.keyDown(document, {key: 'a'});

        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();
    });

    it('ignores the press while no transition is selected', () => {
        renderEditor([buildTransitionEdge({selected: false})]);

        fireEvent.keyDown(document, {key: 'Delete'});

        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();
    });

    it('leaves the press to the field the user is typing in', () => {
        renderEditor([buildTransitionEdge()]);

        const cleanUp = focusMarkup('<input data-focus />');

        fireEvent.keyDown(document, {key: 'Backspace'});

        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();

        cleanUp();
    });

    it('leaves the press to the transition editor To picker', () => {
        renderEditor([buildTransitionEdge()]);

        const cleanUp = focusMarkup('<button data-focus role="combobox">task_3</button>');

        fireEvent.keyDown(document, {key: 'Delete'});

        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();

        cleanUp();
    });

    it('leaves the press to a dialog opened while an edge stayed selected', () => {
        renderEditor([buildTransitionEdge()]);

        const cleanUp = focusMarkup('<div role="dialog"><button data-focus>Save</button></div>');

        fireEvent.keyDown(document, {key: 'Backspace'});

        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();

        cleanUp();
    });

    it('deletes nothing on a read-only canvas', () => {
        renderEditor([buildTransitionEdge()], true);

        fireEvent.keyDown(document, {key: 'Delete'});

        expect(saveGraphTransitionsMock).not.toHaveBeenCalled();
    });
});
