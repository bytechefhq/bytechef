import {Workflow} from '@/shared/middleware/platform/configuration';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore, {runWithoutHistory, setWorkflowWithoutHistory} from '../useWorkflowDataStore';

const COALESCE_MS = 100;

function makeWorkflow(definition: string, version = 1): Workflow {
    return {
        definition,
        id: 'wf-1',
        triggers: [{name: 'trigger_1', type: 'manual/v1/trigger'}],
        version,
    } as Workflow;
}

function definitionOf(value: string): string {
    return JSON.stringify({tasks: [], value});
}

function temporal() {
    return useWorkflowDataStore.temporal.getState();
}

describe('useWorkflowDataStore undo/redo history', () => {
    beforeEach(() => {
        vi.useFakeTimers();

        temporal().pause();

        useWorkflowDataStore.setState({workflow: {nodeNames: ['trigger_1']}});

        temporal().clear();
        temporal().resume();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('records an undo step when the definition changes', () => {
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('a')));

        vi.advanceTimersByTime(COALESCE_MS);

        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('b')));

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(2);
    });

    it('does not record when only the version changes', () => {
        const definition = definitionOf('a');

        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definition, 1));

        vi.advanceTimersByTime(COALESCE_MS);

        const lengthAfterFirstEdit = temporal().pastStates.length;

        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definition, 2));

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(lengthAfterFirstEdit);
        expect(useWorkflowDataStore.getState().workflow.version).toBe(2);
    });

    it('coalesces rapid consecutive changes into a single undo step', () => {
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('a')));
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('b')));
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('c')));

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(1);
    });

    it('restores the previous definition on undo', () => {
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('a')));

        vi.advanceTimersByTime(COALESCE_MS);

        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('b')));

        vi.advanceTimersByTime(COALESCE_MS);

        temporal().undo();

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionOf('a'));

        temporal().redo();

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionOf('b'));
    });

    it('does not record when using setWorkflowWithoutHistory', () => {
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('a')));

        vi.advanceTimersByTime(COALESCE_MS);

        const lengthAfterEdit = temporal().pastStates.length;

        setWorkflowWithoutHistory(makeWorkflow(definitionOf('b')));

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(lengthAfterEdit);
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionOf('b'));
    });

    it('clears history when setWorkflowWithoutHistory is called with clearHistory', () => {
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('a')));

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBeGreaterThan(0);

        setWorkflowWithoutHistory(makeWorkflow(definitionOf('b')), {clearHistory: true});

        expect(temporal().pastStates.length).toBe(0);
    });

    it('does not record changes wrapped in runWithoutHistory', () => {
        useWorkflowDataStore.getState().setWorkflow(makeWorkflow(definitionOf('a')));

        vi.advanceTimersByTime(COALESCE_MS);

        const lengthAfterEdit = temporal().pastStates.length;

        runWithoutHistory(() => {
            useWorkflowDataStore.setState((state) => ({
                workflow: {
                    ...state.workflow,
                    definition: definitionOf('b'),
                },
            }));
        });

        vi.advanceTimersByTime(COALESCE_MS);

        expect(temporal().pastStates.length).toBe(lengthAfterEdit);
    });
});
