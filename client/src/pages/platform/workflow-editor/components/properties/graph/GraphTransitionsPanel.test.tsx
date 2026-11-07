import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType} from '@/shared/types';
import {fireEvent, render, screen, within} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../../stores/useWorkflowDataStore';
import GraphTransitionsPanel from './GraphTransitionsPanel';

const {saveGraphTransitionsMock, updateWorkflowMutationMock} = vi.hoisted(() => ({
    saveGraphTransitionsMock: vi.fn(),
    updateWorkflowMutationMock: {isPending: false, mutate: vi.fn()},
}));

vi.mock('../../../utils/graph/saveGraphParameters', () => ({saveGraphTransitions: saveGraphTransitionsMock}));

vi.mock('../../../providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: updateWorkflowMutationMock}),
}));

const GRAPH_ID = 'graph_1';

function renderPanel(memberNames: string[], transitions: GraphTransitionType[], startNode?: string) {
    const graphTask: WorkflowTask = {
        name: GRAPH_ID,
        parameters: {
            nodes: memberNames.map((memberName) => ({name: memberName, type: 'random/v1/randomInt'})),
            ...(startNode === undefined ? {} : {startNode}),
            transitions,
        },
        type: 'graph/v1',
    };

    useWorkflowDataStore.setState({workflow: {nodeNames: [], tasks: [graphTask]}});

    return render(<GraphTransitionsPanel graphId={GRAPH_ID} />);
}

function applyLastSavedMutation(transitions: Array<GraphTransitionType>): Array<GraphTransitionType> {
    const [{graphId, mutate}] = saveGraphTransitionsMock.mock.calls.at(-1)!;

    expect(graphId).toBe(GRAPH_ID);

    return (mutate as (currentTransitions: Array<GraphTransitionType>) => Array<GraphTransitionType>)(transitions);
}

describe('GraphTransitionsPanel', () => {
    beforeEach(() => {
        saveGraphTransitionsMock.mockClear();
        useWorkflowDataStore.setState({workflow: {nodeNames: []}});
    });

    it('groups every transition under the node it leaves', () => {
        renderPanel(
            ['task_1', 'task_2'],
            [
                {condition: '=x > 1', from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_1'},
                {from: 'task_1', to: 'task_1'},
            ]
        );

        const firstGroup = screen.getByRole('group', {name: 'task_1 transitions'});
        const secondGroup = screen.getByRole('group', {name: 'task_2 transitions'});

        expect(
            within(firstGroup)
                .getAllByRole('listitem')
                .map((row) => row.getAttribute('aria-label'))
        ).toEqual(['Transition task_1 to task_2', 'Transition task_1 to task_1']);

        expect(
            within(secondGroup)
                .getAllByRole('listitem')
                .map((row) => row.getAttribute('aria-label'))
        ).toEqual(['Transition task_2 to task_1']);

        expect(within(firstGroup).getByText('=x > 1')).toBeInTheDocument();
    });

    it('lets a group shrink to the panel width so a long condition truncates instead of widening it', () => {
        const condition = '=${judge.answers.answers_question.isTrue} && ${judge.answers.is_plausible.isTrue}';

        renderPanel(['task_1', 'task_2'], [{condition, from: 'task_1', to: 'task_2'}]);

        expect(screen.getByRole('group', {name: 'task_1 transitions'})).toHaveClass('min-w-0');
        expect(screen.getByText(condition)).toHaveClass('truncate');
    });

    it('labels a node with no outgoing transition as terminal', () => {
        renderPanel(['task_1', 'task_2'], [{from: 'task_1', to: 'task_2'}]);

        const terminalGroup = screen.getByRole('group', {name: 'task_2 transitions'});

        expect(within(terminalGroup).getByText('terminal')).toBeInTheDocument();
        expect(
            within(terminalGroup).getByText('No outgoing transitions - the graph run ends here.')
        ).toBeInTheDocument();

        expect(
            within(screen.getByRole('group', {name: 'task_1 transitions'})).queryByText('terminal')
        ).not.toBeInTheDocument();
    });

    it('warns on a node with more than one unconditional transition', () => {
        renderPanel(
            ['task_1', 'task_2', 'task_3'],
            [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_1', to: 'task_3'},
            ]
        );

        expect(
            screen.getByText('More than one unconditional transition - the first declared is taken.')
        ).toBeInTheDocument();
    });

    it('leaves a node with one unconditional transition beside conditional ones unwarned', () => {
        renderPanel(
            ['task_1', 'task_2', 'task_3'],
            [
                {condition: '=x > 1', from: 'task_1', to: 'task_2'},
                {from: 'task_1', to: 'task_3'},
            ]
        );

        expect(
            screen.queryByText('More than one unconditional transition - the first declared is taken.')
        ).not.toBeInTheDocument();
    });

    it('surfaces a transition leaving a node the graph no longer declares', () => {
        renderPanel(['task_1'], [{from: 'deleted_task', to: 'task_1'}]);

        const danglingGroup = screen.getByRole('group', {name: 'deleted_task transitions'});

        expect(
            within(danglingGroup).getByText(
                'The graph declares no node named "deleted_task", so the transitions below cannot be drawn on the canvas.'
            )
        ).toBeInTheDocument();

        expect(within(danglingGroup).getByRole('listitem')).toHaveAttribute(
            'aria-label',
            'Transition deleted_task to task_1'
        );
    });

    it('warns once for a group of several transitions leaving the same undeclared node', () => {
        renderPanel(
            ['task_1', 'task_2'],
            [
                {from: 'deleted_task', to: 'task_1'},
                {from: 'deleted_task', to: 'task_2'},
            ]
        );

        const danglingGroup = screen.getByRole('group', {name: 'deleted_task transitions'});

        expect(within(danglingGroup).getAllByRole('listitem')).toHaveLength(2);
        expect(
            within(danglingGroup).getAllByText(
                'The graph declares no node named "deleted_task", so the transitions below cannot be drawn on the canvas.'
            )
        ).toHaveLength(1);
    });

    it('surfaces a transition pointing at a node the graph no longer declares', () => {
        renderPanel(['task_1'], [{from: 'task_1', to: 'deleted_task'}]);

        expect(
            screen.getByText('The graph declares no node named "deleted_task", so this transition cannot be drawn.')
        ).toBeInTheDocument();
    });

    it('does not mistake an expression target for a missing node', () => {
        renderPanel(['task_1'], [{from: 'task_1', to: '=nextStep'}]);

        expect(screen.queryByText(/cannot be drawn/)).not.toBeInTheDocument();
        expect(screen.getByText('dynamic')).toBeInTheDocument();
    });

    it('removes the transition its row deletes, leaving the others in place', () => {
        renderPanel(
            ['task_1', 'task_2'],
            [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_1'},
            ]
        );

        fireEvent.click(screen.getByRole('button', {name: 'Delete transition task_1 to task_2'}));

        expect(
            applyLastSavedMutation([
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_1'},
            ])
        ).toEqual([{from: 'task_2', to: 'task_1'}]);
    });

    it('lists the conditional transitions of a node before its unconditional one, as the run evaluates them', () => {
        renderPanel(
            ['task_1', 'task_2', 'task_3'],
            [
                {from: 'task_1', to: 'task_3'},
                {condition: '=x > 1', from: 'task_1', to: 'task_2'},
            ]
        );

        expect(
            within(screen.getByRole('group', {name: 'task_1 transitions'}))
                .getAllByRole('listitem')
                .map((row) => row.getAttribute('aria-label'))
        ).toEqual(['Transition task_1 to task_2', 'Transition task_1 to task_3']);
    });

    it('deletes the declared transition of a row that is listed out of declaration order', () => {
        const transitions = [
            {from: 'task_1', to: 'task_3'},
            {condition: '=x > 1', from: 'task_1', to: 'task_2'},
        ];

        renderPanel(['task_1', 'task_2', 'task_3'], transitions);

        fireEvent.click(screen.getByRole('button', {name: 'Delete transition task_1 to task_2'}));

        expect(applyLastSavedMutation(transitions)).toEqual([{from: 'task_1', to: 'task_3'}]);
    });

    it('reorders a conditional transition among the conditional transitions of its node when moved down', () => {
        const transitions = [
            {condition: '=x > 1', from: 'task_1', to: 'task_2'},
            {from: 'task_1', to: 'task_1'},
            {condition: '=x > 5', from: 'task_1', to: 'task_3'},
        ];

        renderPanel(['task_1', 'task_2', 'task_3'], transitions);

        fireEvent.click(screen.getByRole('button', {name: 'Move transition task_1 to task_2 down'}));

        expect(applyLastSavedMutation(transitions)).toEqual([
            {condition: '=x > 5', from: 'task_1', to: 'task_3'},
            {from: 'task_1', to: 'task_1'},
            {condition: '=x > 1', from: 'task_1', to: 'task_2'},
        ]);
    });

    it('offers no reordering between a conditional transition and the unconditional one', () => {
        renderPanel(
            ['task_1', 'task_2', 'task_3'],
            [
                {condition: '=x > 1', from: 'task_1', to: 'task_2'},
                {from: 'task_1', to: 'task_3'},
            ]
        );

        expect(screen.queryByRole('button', {name: /^Move transition/})).not.toBeInTheDocument();
    });

    it('leaves the list alone when another transition has shifted into the row index', () => {
        renderPanel(
            ['task_1', 'task_2'],
            [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_2', to: 'task_1'},
            ]
        );

        fireEvent.click(screen.getByRole('button', {name: 'Delete transition task_1 to task_2'}));

        const shiftedTransitions = [{from: 'task_2', to: 'task_1'}];

        expect(applyLastSavedMutation(shiftedTransitions)).toBe(shiftedTransitions);
    });

    it('warns when the start node names no node the graph declares', () => {
        renderPanel(['task_1', 'task_2'], [{from: 'task_1', to: 'task_2'}], 'deleted_task');

        expect(
            screen.getByText(
                'The graph declares no node named "deleted_task", so a run cannot start there. Pick a start node on the canvas.'
            )
        ).toBeInTheDocument();
    });

    it('leaves a declared or unset start node unwarned', () => {
        renderPanel(['task_1', 'task_2'], [{from: 'task_1', to: 'task_2'}], 'task_2');

        expect(screen.queryByText(/a run cannot start there/)).not.toBeInTheDocument();
    });

    it('disables the move buttons at the bounds of a group', () => {
        renderPanel(
            ['task_1', 'task_2', 'task_3'],
            [
                {from: 'task_1', to: 'task_2'},
                {from: 'task_1', to: 'task_3'},
            ]
        );

        expect(screen.getByRole('button', {name: 'Move transition task_1 to task_2 up'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Move transition task_1 to task_2 down'})).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Move transition task_1 to task_3 down'})).toBeDisabled();
    });

    it('explains an empty graph rather than rendering an empty list', () => {
        renderPanel([], []);

        expect(screen.getByText(/no nodes yet/)).toBeInTheDocument();
    });
});
