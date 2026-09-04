import {WorkflowInput} from '@/shared/middleware/platform/configuration';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {
    clearAllWorkflowMutations,
    drainPendingSaves,
    hasPendingSaves,
    setWorkflowMutating,
} from '../../../utils/workflowMutationGuard';
import useWorkflowInputs from './useWorkflowInputs';

const {saveTestConfigurationInputsMock, updateWorkflowMutationMock, workflowDataState} = vi.hoisted(() => ({
    saveTestConfigurationInputsMock: vi.fn(),
    updateWorkflowMutationMock: {mutate: vi.fn()},
    workflowDataState: {
        setWorkflow: vi.fn(),
        workflow: {definition: '', id: 'workflow-1', inputs: [] as WorkflowInput[], version: 1},
    },
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: updateWorkflowMutationMock}),
}));

vi.mock('@/shared/mutations/platform/workflowTestConfigurations.mutations', () => ({
    useSaveWorkflowTestConfigurationInputsMutation: () => ({mutate: saveTestConfigurationInputsMock}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('../../../stores/useWorkflowDataStore', () => ({
    default: Object.assign((selector: (state: typeof workflowDataState) => unknown) => selector(workflowDataState), {
        getState: () => workflowDataState,
    }),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

function setDefinition(inputs: Array<WorkflowInput>) {
    workflowDataState.workflow = {
        ...workflowDataState.workflow,
        definition: JSON.stringify({inputs, tasks: []}),
        inputs,
    };
}

function sentDefinition(call = 0) {
    return JSON.parse(updateWorkflowMutationMock.mutate.mock.calls[call][0].workflow.definition);
}

function renderInputs() {
    return renderHook(() => useWorkflowInputs({invalidateWorkflowQueries: vi.fn()}));
}

describe('useWorkflowInputs', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        setDefinition([]);
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('appends a new input and sends it through the guarded save', () => {
        const {result} = renderInputs();

        act(() => {
            result.current.saveWorkflowInput({label: 'City', name: 'city', required: false, type: 'string'} as never);
        });

        expect(updateWorkflowMutationMock.mutate).toHaveBeenCalledOnce();
        expect(sentDefinition().inputs.map((input: WorkflowInput) => input.name)).toEqual(['city']);
    });

    it('renames a duplicate rather than overwriting the input already there', () => {
        setDefinition([{label: 'City', name: 'city', required: false, type: 'string'} as WorkflowInput]);

        const {result} = renderInputs();

        act(() => {
            result.current.saveWorkflowInput({label: 'City', name: 'city', required: false, type: 'string'} as never);
        });

        expect(sentDefinition().inputs.map((input: WorkflowInput) => input.name)).toEqual(['city', 'city_1']);
    });

    it('queues the save behind an in-flight one and builds on what that save left behind', () => {
        const {result} = renderInputs();

        setWorkflowMutating('workflow-1', true);

        act(() => {
            result.current.saveWorkflowInput({label: 'City', name: 'city', required: false, type: 'string'} as never);
        });

        expect(updateWorkflowMutationMock.mutate).not.toHaveBeenCalled();
        expect(hasPendingSaves('workflow-1')).toBe(true);

        // The in-flight save added an input of its own; the queued one must not drop it.
        setDefinition([{label: 'Country', name: 'country', required: false, type: 'string'} as WorkflowInput]);

        setWorkflowMutating('workflow-1', false);

        drainPendingSaves('workflow-1');

        expect(updateWorkflowMutationMock.mutate).toHaveBeenCalledOnce();
        expect(sentDefinition().inputs.map((input: WorkflowInput) => input.name)).toEqual(['country', 'city']);
    });

    it('removes an input by name on delete', () => {
        setDefinition([
            {label: 'City', name: 'city', required: false, type: 'string'} as WorkflowInput,
            {label: 'Country', name: 'country', required: false, type: 'string'} as WorkflowInput,
        ]);

        const {result} = renderInputs();

        act(() => {
            result.current.deleteWorkflowInput({name: 'city'} as WorkflowInput);
        });

        expect(sentDefinition().inputs.map((input: WorkflowInput) => input.name)).toEqual(['country']);
    });

    it('rolls the store back when a delete fails', () => {
        const originalInputs = [{label: 'City', name: 'city', required: false, type: 'string'} as WorkflowInput];

        setDefinition(originalInputs);

        const {result} = renderInputs();

        act(() => {
            result.current.deleteWorkflowInput({name: 'city'} as WorkflowInput);
        });

        const callbacks = updateWorkflowMutationMock.mutate.mock.calls[0][1];

        act(() => {
            callbacks.onError(new Error('version conflict'));
        });

        const restored = workflowDataState.setWorkflow.mock.calls.at(-1)?.[0];

        expect(restored.inputs.map((input: WorkflowInput) => input.name)).toEqual(['city']);
    });

    it('replaces the input at the index being edited', () => {
        setDefinition([
            {label: 'City', name: 'city', required: false, type: 'string'} as WorkflowInput,
            {label: 'Country', name: 'country', required: false, type: 'string'} as WorkflowInput,
        ]);

        const {result} = renderInputs();

        act(() => {
            result.current.openEditDialog(1);
        });

        act(() => {
            result.current.saveWorkflowInput({
                label: 'Region',
                name: 'region',
                required: false,
                type: 'string',
            } as never);
        });

        expect(sentDefinition().inputs.map((input: WorkflowInput) => input.name)).toEqual(['city', 'region']);
    });

    it('stores the test value and refocuses the name field once the save lands', () => {
        vi.useFakeTimers();

        const nameInput = document.createElement('input');

        nameInput.setAttribute('name', 'name');
        document.body.appendChild(nameInput);

        const {result} = renderInputs();

        act(() => {
            result.current.saveWorkflowInput({label: 'City', name: 'city', required: false, type: 'string'} as never);
        });

        const callbacks = updateWorkflowMutationMock.mutate.mock.calls[0][1];

        act(() => {
            callbacks.onSuccess({id: 'workflow-1', version: 2});
        });

        act(() => {
            vi.runAllTimers();
        });

        expect(saveTestConfigurationInputsMock).toHaveBeenCalledOnce();
        expect(document.activeElement).toBe(nameInput);

        document.body.removeChild(nameInput);

        vi.useRealTimers();
    });

    it('rolls the store back when a save fails', () => {
        setDefinition([{label: 'City', name: 'city', required: false, type: 'string'} as WorkflowInput]);

        const {result} = renderInputs();

        act(() => {
            result.current.saveWorkflowInput({
                label: 'Country',
                name: 'country',
                required: false,
                type: 'string',
            } as never);
        });

        const callbacks = updateWorkflowMutationMock.mutate.mock.calls[0][1];

        act(() => {
            callbacks.onError(new Error('version conflict'));
        });

        const restored = workflowDataState.setWorkflow.mock.calls.at(-1)?.[0];

        expect(restored.inputs.map((input: WorkflowInput) => input.name)).toEqual(['city']);
    });

    it('closes the delete dialog once the delete lands', () => {
        setDefinition([{label: 'City', name: 'city', required: false, type: 'string'} as WorkflowInput]);

        const {result} = renderInputs();

        act(() => {
            result.current.openDeleteDialog(0);
        });

        expect(result.current.isDeleteDialogOpen).toBe(true);

        act(() => {
            result.current.deleteWorkflowInput({name: 'city'} as WorkflowInput);
        });

        const callbacks = updateWorkflowMutationMock.mutate.mock.calls[0][1];

        act(() => {
            callbacks.onSuccess({id: 'workflow-1', version: 2});
        });

        expect(result.current.isDeleteDialogOpen).toBe(false);
    });
});
