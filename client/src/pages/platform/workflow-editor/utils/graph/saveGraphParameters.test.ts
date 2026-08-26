import {UpdateWorkflowMutationType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {saveGraphParameters, saveGraphTransitions} from './saveGraphParameters';

// saveGraphParameters is thin glue: parse the store's definition, locate the graph task, apply the
// updater, hand the recursively-rewritten task list to saveWorkflowDefinition. These tests pin
// that glue (found/not-found/malformed-JSON branches) without re-covering saveWorkflowDefinition's
// own mutation/rollback behaviour, which already has its own test file.
const {mockWorkflowState, saveWorkflowDefinitionMock} = vi.hoisted(() => ({
    mockWorkflowState: {workflow: {definition: undefined as string | undefined}},
    saveWorkflowDefinitionMock: vi.fn(),
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: {
        getState: () => mockWorkflowState,
    },
}));

vi.mock('../saveWorkflowDefinition', () => ({
    default: saveWorkflowDefinitionMock,
}));

function makeMutation(): UpdateWorkflowMutationType {
    return {mutate: vi.fn()} as unknown as UpdateWorkflowMutationType;
}

describe('saveGraphParameters', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockWorkflowState.workflow = {definition: undefined};
    });

    it('applies the updater to the graph task parameters and forwards the rewritten tasks', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [
                    {name: 'other_1', parameters: {}, type: 'httpClient/v1/get'},
                    {name: 'graph_1', parameters: {nodes: [], transitions: []}, type: 'graph/v1'},
                ],
            }),
        };

        const mutation = makeMutation();

        saveGraphParameters(
            'graph_1',
            (parameters) => ({...parameters, transitions: [{from: 'a', to: 'b'}]}),
            mutation
        );

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();

        const call = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(call.updateWorkflowMutation).toBe(mutation);
        expect(call.updatedWorkflowTasks).toHaveLength(2);
        expect(call.updatedWorkflowTasks[0].name).toBe('other_1');
        expect(call.updatedWorkflowTasks[1]).toMatchObject({
            name: 'graph_1',
            parameters: {nodes: [], transitions: [{from: 'a', to: 'b'}]},
        });
    });

    it('does nothing when the graph task cannot be found', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({tasks: [{name: 'other_1', parameters: {}, type: 'httpClient/v1/get'}]}),
        };

        saveGraphParameters('missing_graph', (parameters) => parameters, makeMutation());

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('does nothing when the store has no definition yet', () => {
        mockWorkflowState.workflow = {definition: undefined};

        saveGraphParameters('graph_1', (parameters) => parameters, makeMutation());

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('does nothing when the stored definition is not valid JSON', () => {
        mockWorkflowState.workflow = {definition: '{not json'};

        saveGraphParameters('graph_1', (parameters) => parameters, makeMutation());

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });
});

describe('saveGraphTransitions', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockWorkflowState.workflow = {definition: undefined};
    });

    it('replaces only the transitions list, leaving every other graph parameter in place', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [
                    {
                        name: 'graph_1',
                        parameters: {
                            maxTransitions: 50,
                            nodes: [{name: 'task_1'}],
                            startNode: 'task_1',
                            transitions: [
                                {from: 'task_1', to: 'task_2'},
                                {from: 'task_2', to: 'task_1'},
                            ],
                        },
                        type: 'graph/v1',
                    },
                ],
            }),
        };

        saveGraphTransitions('graph_1', (transitions) => transitions.slice(1), makeMutation());

        expect(saveWorkflowDefinitionMock.mock.calls[0][0].updatedWorkflowTasks[0].parameters).toEqual({
            maxTransitions: 50,
            nodes: [{name: 'task_1'}],
            startNode: 'task_1',
            transitions: [{from: 'task_2', to: 'task_1'}],
        });
    });

    it('hands the mutation an empty list when the graph declares no transitions yet', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'graph_1', parameters: {nodes: []}, type: 'graph/v1'}],
            }),
        };

        saveGraphTransitions(
            'graph_1',
            (transitions) => [...transitions, {from: 'task_1', to: 'task_2'}],
            makeMutation()
        );

        expect(saveWorkflowDefinitionMock.mock.calls[0][0].updatedWorkflowTasks[0].parameters).toEqual({
            nodes: [],
            transitions: [{from: 'task_1', to: 'task_2'}],
        });
    });
});
