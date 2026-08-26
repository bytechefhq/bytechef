import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowTestNodeStates from './useWorkflowTestNodeStates';

function setLoadedWorkflowId(workflowId: string | undefined) {
    useWorkflowDataStore.setState((state) => ({
        workflow: {...state.workflow, id: workflowId},
    }));
}

function recordRunFor(workflowId: string) {
    const {resetWorkflowTestNodeStates, setWorkflowTestNodeState} = useWorkflowEditorStore.getState();

    resetWorkflowTestNodeStates(workflowId);
    setWorkflowTestNodeState('var_1', {status: 'COMPLETED'});
}

describe('useWorkflowTestNodeStates', () => {
    beforeEach(() => {
        useWorkflowEditorStore.getState().resetWorkflowTestNodeStates(undefined);

        setLoadedWorkflowId(undefined);
    });

    it('returns the node states recorded for the loaded workflow', () => {
        recordRunFor('workflow-a');
        setLoadedWorkflowId('workflow-a');

        const {result} = renderHook(() => useWorkflowTestNodeStates());

        expect(result.current).toEqual({var_1: {status: 'COMPLETED'}});
    });

    it('returns no node states when they were recorded for a different workflow', () => {
        recordRunFor('workflow-a');
        setLoadedWorkflowId('workflow-b');

        const {result} = renderHook(() => useWorkflowTestNodeStates());

        expect(result.current).toEqual({});
    });

    it('returns the same empty object across renders so memoized consumers do not churn', () => {
        recordRunFor('workflow-a');
        setLoadedWorkflowId('workflow-b');

        const {rerender, result} = renderHook(() => useWorkflowTestNodeStates());

        const firstResult = result.current;

        rerender();

        expect(result.current).toBe(firstResult);
    });
});
