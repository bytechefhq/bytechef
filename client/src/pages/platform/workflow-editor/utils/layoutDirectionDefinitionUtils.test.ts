import {UpdateWorkflowMutationType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {
    applyLayoutDirectionToDefinition,
    extractLayoutDirection,
    saveLayoutDirection,
} from './layoutDirectionDefinitionUtils';
import {clearAllWorkflowMutations, drainPendingSaves, setWorkflowMutating} from './workflowMutationGuard';

const makeDefinition = (metadata?: object) =>
    JSON.stringify({
        label: 'Workflow',
        tasks: [{name: 'task_1', type: 'component/v1/action'}],
        ...(metadata ? {metadata} : {}),
    });

describe('extractLayoutDirection', () => {
    it('returns the direction stored under metadata.ui', () => {
        expect(extractLayoutDirection(makeDefinition({ui: {layoutDirection: 'LR'}}))).toBe('LR');
        expect(extractLayoutDirection(makeDefinition({ui: {layoutDirection: 'TB'}}))).toBe('TB');
    });

    it('returns undefined when no direction is stored', () => {
        expect(extractLayoutDirection(makeDefinition())).toBeUndefined();
        expect(extractLayoutDirection(makeDefinition({ui: {stickyNotes: []}}))).toBeUndefined();
    });

    it('returns undefined for an unknown direction value', () => {
        expect(extractLayoutDirection(makeDefinition({ui: {layoutDirection: 'RL'}}))).toBeUndefined();
    });

    it('returns undefined for a missing or unparsable definition', () => {
        expect(extractLayoutDirection(undefined)).toBeUndefined();
        expect(extractLayoutDirection('')).toBeUndefined();
        expect(extractLayoutDirection('{not json')).toBeUndefined();
    });
});

describe('applyLayoutDirectionToDefinition', () => {
    it('stamps a non-default direction and keeps the rest of metadata.ui', () => {
        const stickyNotes = [{content: 'note', id: 'stickyNote_1', position: {x: 1, y: 2}}];

        const updatedDefinition = applyLayoutDirectionToDefinition(
            makeDefinition({other: true, ui: {stickyNotes}}),
            'LR'
        );

        const parsedDefinition = JSON.parse(updatedDefinition);

        expect(parsedDefinition.metadata).toEqual({other: true, ui: {layoutDirection: 'LR', stickyNotes}});
        expect(parsedDefinition.tasks).toEqual([{name: 'task_1', type: 'component/v1/action'}]);
    });

    it('leaves a definition without a direction untouched for the default direction', () => {
        const definition = makeDefinition();

        expect(applyLayoutDirectionToDefinition(definition, 'TB')).toBe(definition);
    });

    it('leaves a definition untouched when it already has the direction', () => {
        const definition = makeDefinition({ui: {layoutDirection: 'LR'}});

        expect(applyLayoutDirectionToDefinition(definition, 'LR')).toBe(definition);
    });

    it('overwrites a stored direction when switching back to the default', () => {
        const updatedDefinition = applyLayoutDirectionToDefinition(makeDefinition({ui: {layoutDirection: 'LR'}}), 'TB');

        expect(extractLayoutDirection(updatedDefinition)).toBe('TB');
    });

    it('returns an unparsable definition unchanged', () => {
        expect(applyLayoutDirectionToDefinition('{not json', 'LR')).toBe('{not json');
    });
});

describe('saveLayoutDirection', () => {
    const workflowId = 'workflow_1';

    let mutateMock: ReturnType<typeof vi.fn>;
    let updateWorkflowMutation: UpdateWorkflowMutationType;

    beforeEach(() => {
        clearAllWorkflowMutations();

        useWorkflowDataStore.temporal.getState().clear();

        mutateMock = vi.fn();
        updateWorkflowMutation = {mutate: mutateMock} as unknown as UpdateWorkflowMutationType;

        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: makeDefinition(),
                id: workflowId,
                version: 3,
            },
        }));

        useWorkflowDataStore.temporal.getState().clear();
    });

    it('writes the direction into the stored definition and fires the mutation', () => {
        saveLayoutDirection({layoutDirection: 'LR', updateWorkflowMutation});

        const storedDefinition = useWorkflowDataStore.getState().workflow.definition!;

        expect(extractLayoutDirection(storedDefinition)).toBe('LR');

        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(mutateMock.mock.calls[0][0]).toEqual({
            id: workflowId,
            workflow: {
                definition: storedDefinition,
                version: 3,
            },
        });
    });

    it('does not record an undo step', () => {
        saveLayoutDirection({layoutDirection: 'LR', updateWorkflowMutation});

        expect(useWorkflowDataStore.temporal.getState().pastStates).toHaveLength(0);
    });

    it('does nothing when the definition already renders in that direction', () => {
        const definition = useWorkflowDataStore.getState().workflow.definition;

        saveLayoutDirection({layoutDirection: 'TB', updateWorkflowMutation});

        expect(mutateMock).not.toHaveBeenCalled();
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definition);
    });

    it('does nothing when the workflow has no definition', () => {
        useWorkflowDataStore.setState((state) => ({workflow: {...state.workflow, definition: undefined}}));

        saveLayoutDirection({layoutDirection: 'LR', updateWorkflowMutation});

        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('queues the save while another workflow mutation is in flight and runs it once drained', () => {
        setWorkflowMutating(workflowId, true);

        saveLayoutDirection({layoutDirection: 'LR', updateWorkflowMutation});

        expect(mutateMock).not.toHaveBeenCalled();
        expect(extractLayoutDirection(useWorkflowDataStore.getState().workflow.definition)).toBeUndefined();

        setWorkflowMutating(workflowId, false);

        drainPendingSaves(workflowId);

        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(extractLayoutDirection(useWorkflowDataStore.getState().workflow.definition)).toBe('LR');
    });

    it('restores the previous definition when the mutation fails', () => {
        const previousDefinition = useWorkflowDataStore.getState().workflow.definition;

        saveLayoutDirection({layoutDirection: 'LR', updateWorkflowMutation});

        mutateMock.mock.calls[0][1].onError(new Error('boom'));

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(previousDefinition);
    });
});
