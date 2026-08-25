import {act, renderHook} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import useWorkflowInputs from './useWorkflowInputs';

const {updateWorkflowMutationMock} = vi.hoisted(() => ({
    updateWorkflowMutationMock: {mutate: vi.fn()},
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: updateWorkflowMutationMock}),
}));

vi.mock('@/shared/mutations/platform/workflowTestConfigurations.mutations', () => ({
    useSaveWorkflowTestConfigurationInputsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('../../../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: {setWorkflow: () => void; workflow: unknown}) => unknown) =>
        selector({
            setWorkflow: vi.fn(),
            workflow: {definition: JSON.stringify({inputs: []}), id: 'workflow-1', version: 1},
        }),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

describe('useWorkflowInputs', () => {
    it('rejects "vars" as a reserved input name and does not save', () => {
        updateWorkflowMutationMock.mutate.mockClear();

        const {result} = renderHook(() => useWorkflowInputs({invalidateWorkflowQueries: vi.fn()}));

        act(() => {
            result.current.saveWorkflowInput({label: 'Vars', name: 'vars', required: false, type: 'string'} as never);
        });

        expect(updateWorkflowMutationMock.mutate).not.toHaveBeenCalled();
        expect(result.current.form.getFieldState('name').error?.message).toBe('"vars" is a reserved name.');
    });

    it('accepts "varsCount" as an input name', () => {
        updateWorkflowMutationMock.mutate.mockClear();

        const {result} = renderHook(() => useWorkflowInputs({invalidateWorkflowQueries: vi.fn()}));

        act(() => {
            result.current.saveWorkflowInput({
                label: 'Vars Count',
                name: 'varsCount',
                required: false,
                type: 'string',
            } as never);
        });

        expect(updateWorkflowMutationMock.mutate).toHaveBeenCalled();
    });
});
