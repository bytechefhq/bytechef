import {UpdateWorkflowMutationType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    clearAllWorkflowMutations,
    drainPendingSaves,
    hasPendingSaves,
    setWorkflowMutating,
} from '../workflowMutationGuard';
import {saveGraphParameters, saveGraphTransitions} from './saveGraphParameters';

const {mockWorkflowState, saveWorkflowDefinitionMock} = vi.hoisted(() => ({
    mockWorkflowState: {workflow: {definition: undefined} as {definition?: string; id?: string}},
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

        clearAllWorkflowMutations();

        mockWorkflowState.workflow = {definition: undefined};
    });

    it('re-runs itself from the fresh definition once an in-flight mutation releases the guard', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'graph_1', parameters: {transitions: []}, type: 'graph/v1'}],
            }),
            id: 'workflow-1',
        };

        const mutation = makeMutation();

        setWorkflowMutating('workflow-1', true);

        saveGraphParameters({
            graphId: 'graph_1',
            updateWorkflowMutation: mutation,
            updater: (parameters) => ({...parameters, transitions: [{from: 'a', to: 'b'}]}),
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(hasPendingSaves('workflow-1')).toBe(true);

        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'graph_1', parameters: {maxTransitions: 5, transitions: []}, type: 'graph/v1'}],
            }),
            id: 'workflow-1',
        };

        setWorkflowMutating('workflow-1', false);

        drainPendingSaves('workflow-1');

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();

        const call = saveWorkflowDefinitionMock.mock.calls[0][0];

        expect(call.updatedWorkflowTasks[0].parameters).toEqual({
            maxTransitions: 5,
            transitions: [{from: 'a', to: 'b'}],
        });
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

        saveGraphParameters({
            graphId: 'graph_1',
            updateWorkflowMutation: mutation,
            updater: (parameters) => ({...parameters, transitions: [{from: 'a', to: 'b'}]}),
        });

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

        saveGraphParameters({
            graphId: 'missing_graph',
            updateWorkflowMutation: makeMutation(),
            updater: (parameters) => parameters,
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('does nothing when the store has no definition yet', () => {
        mockWorkflowState.workflow = {definition: undefined};

        saveGraphParameters({
            graphId: 'graph_1',
            updateWorkflowMutation: makeMutation(),
            updater: (parameters) => parameters,
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('does nothing when the stored definition is not valid JSON', () => {
        mockWorkflowState.workflow = {definition: '{not json'};

        saveGraphParameters({
            graphId: 'graph_1',
            updateWorkflowMutation: makeMutation(),
            updater: (parameters) => parameters,
        });

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

        saveGraphTransitions({
            graphId: 'graph_1',
            mutate: (transitions) => transitions.slice(1),
            updateWorkflowMutation: makeMutation(),
        });

        expect(saveWorkflowDefinitionMock.mock.calls[0][0].updatedWorkflowTasks[0].parameters).toEqual({
            maxTransitions: 50,
            nodes: [{name: 'task_1'}],
            startNode: 'task_1',
            transitions: [{from: 'task_2', to: 'task_1'}],
        });
    });

    it('saves nothing when the mutation hands the transitions back untouched', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [
                    {
                        name: 'graph_1',
                        parameters: {nodes: [{name: 'task_1'}], transitions: [{from: 'task_1', to: 'task_2'}]},
                        type: 'graph/v1',
                    },
                ],
            }),
        };

        saveGraphTransitions({
            graphId: 'graph_1',
            mutate: (transitions) => transitions,
            updateWorkflowMutation: makeMutation(),
        });

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
    });

    it('hands the mutation an empty list when the graph declares no transitions yet', () => {
        mockWorkflowState.workflow = {
            definition: JSON.stringify({
                tasks: [{name: 'graph_1', parameters: {nodes: []}, type: 'graph/v1'}],
            }),
        };

        saveGraphTransitions({
            graphId: 'graph_1',
            mutate: (transitions) => [...transitions, {from: 'task_1', to: 'task_2'}],
            updateWorkflowMutation: makeMutation(),
        });

        expect(saveWorkflowDefinitionMock.mock.calls[0][0].updatedWorkflowTasks[0].parameters).toEqual({
            nodes: [],
            transitions: [{from: 'task_1', to: 'task_2'}],
        });
    });
});
