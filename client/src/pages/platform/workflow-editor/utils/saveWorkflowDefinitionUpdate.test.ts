import {SPACE} from '@/shared/constants';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import saveWorkflowDefinitionUpdate from './saveWorkflowDefinitionUpdate';
import {
    clearAllWorkflowMutations,
    drainPendingSaves,
    hasPendingSaves,
    isWorkflowMutating,
    setWorkflowMutating,
} from './workflowMutationGuard';

let mockWorkflowState: ReturnType<typeof makeWorkflowState>;

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: {
        getState: () => mockWorkflowState,
    },
}));

function makeWorkflowState(definition: Record<string, unknown>, version = 1) {
    return {
        setWorkflow: vi.fn(),
        workflow: {
            definition: JSON.stringify(definition, null, SPACE),
            id: 'workflow-1',
            version,
        },
    };
}

function makeMutation() {
    return {mutate: vi.fn()} as unknown as Parameters<typeof saveWorkflowDefinitionUpdate>[0]['updateWorkflowMutation'];
}

function sentDefinition(mutation: ReturnType<typeof makeMutation>, call = 0) {
    return JSON.parse((mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[call][0].workflow.definition);
}

function sentVersion(mutation: ReturnType<typeof makeMutation>, call = 0) {
    return (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[call][0].workflow.version;
}

function callbacks(mutation: ReturnType<typeof makeMutation>, call = 0) {
    return (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[call][1];
}

describe('saveWorkflowDefinitionUpdate', () => {
    beforeEach(() => {
        mockWorkflowState = makeWorkflowState({inputs: [], tasks: []});
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('takes the guard, sends the updated definition with the store version, and releases on settle', () => {
        const mutation = makeMutation();

        saveWorkflowDefinitionUpdate({
            updateDefinition: (definition) => ({...definition, inputs: [{name: 'city', type: 'STRING'}]}),
            updateWorkflowMutation: mutation,
        });

        expect(isWorkflowMutating('workflow-1')).toBe(true);
        expect(sentDefinition(mutation).inputs).toEqual([{name: 'city', type: 'STRING'}]);
        expect(sentVersion(mutation)).toBe(1);

        callbacks(mutation).onSettled();

        expect(isWorkflowMutating('workflow-1')).toBe(false);
    });

    it('queues itself while another save is in flight and rebuilds from the fresh definition at drain', () => {
        const mutation = makeMutation();

        setWorkflowMutating('workflow-1', true);

        saveWorkflowDefinitionUpdate({
            updateDefinition: (definition) => ({...definition, inputs: [{name: 'city', type: 'STRING'}]}),
            updateWorkflowMutation: mutation,
        });

        expect(mutation.mutate).not.toHaveBeenCalled();
        expect(hasPendingSaves('workflow-1')).toBe(true);

        mockWorkflowState = makeWorkflowState({inputs: [], tasks: [{name: 'logger_1', type: 'logger/v1/debug'}]}, 2);

        setWorkflowMutating('workflow-1', false);

        drainPendingSaves('workflow-1');

        expect(mutation.mutate).toHaveBeenCalledTimes(1);
        expect(sentDefinition(mutation).tasks).toEqual([{name: 'logger_1', type: 'logger/v1/debug'}]);
        expect(sentDefinition(mutation).inputs).toEqual([{name: 'city', type: 'STRING'}]);
        expect(sentVersion(mutation)).toBe(2);
        expect(isWorkflowMutating('workflow-1')).toBe(true);
    });

    it('drains the next queued save when its own mutation settles', () => {
        const mutation = makeMutation();

        saveWorkflowDefinitionUpdate({
            updateDefinition: (definition) => ({...definition, inputs: [{name: 'first', type: 'STRING'}]}),
            updateWorkflowMutation: mutation,
        });

        saveWorkflowDefinitionUpdate({
            updateDefinition: (definition) => ({...definition, inputs: [{name: 'second', type: 'STRING'}]}),
            updateWorkflowMutation: mutation,
        });

        expect(mutation.mutate).toHaveBeenCalledTimes(1);

        callbacks(mutation).onSettled();

        expect(mutation.mutate).toHaveBeenCalledTimes(2);
        expect(sentDefinition(mutation, 1).inputs).toEqual([{name: 'second', type: 'STRING'}]);
    });

    it('saves nothing when the updater declines, e.g. the node it targets is gone from the fresh definition', () => {
        const mutation = makeMutation();

        saveWorkflowDefinitionUpdate({
            updateDefinition: () => undefined,
            updateWorkflowMutation: mutation,
        });

        expect(mutation.mutate).not.toHaveBeenCalled();
        expect(isWorkflowMutating('workflow-1')).toBe(false);
    });

    it('puts the server response into the store on success, keeping the definition it sent', () => {
        const mutation = makeMutation();
        const onSuccess = vi.fn();

        saveWorkflowDefinitionUpdate({
            onSuccess,
            updateDefinition: (definition) => ({...definition, inputs: [{name: 'city', type: 'STRING'}]}),
            updateWorkflowMutation: mutation,
        });

        const updatedWorkflow = {id: 'workflow-1', inputs: [{name: 'city', type: 'STRING'}], version: 2};

        callbacks(mutation).onSuccess(updatedWorkflow);

        expect(mockWorkflowState.setWorkflow).toHaveBeenCalledOnce();

        const storedWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

        expect(storedWorkflow.version).toBe(2);
        expect(storedWorkflow.inputs).toEqual([{name: 'city', type: 'STRING'}]);
        expect(JSON.parse(storedWorkflow.definition).inputs).toEqual([{name: 'city', type: 'STRING'}]);
        expect(onSuccess).toHaveBeenCalledWith(updatedWorkflow);
    });

    it('forwards a failure to onError and leaves the store alone', () => {
        const mutation = makeMutation();
        const onError = vi.fn();

        saveWorkflowDefinitionUpdate({
            onError,
            updateDefinition: (definition) => definition,
            updateWorkflowMutation: mutation,
        });

        callbacks(mutation).onError(new Error('version conflict'));

        expect(onError).toHaveBeenCalledOnce();
        expect(mockWorkflowState.setWorkflow).not.toHaveBeenCalled();
    });
});
