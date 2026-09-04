import {ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import saveTaskDispatcherSubtaskFieldChange from '../saveTaskDispatcherSubtaskFieldChange';
import {
    clearAllWorkflowMutations,
    drainPendingSaves,
    hasPendingSaves,
    setWorkflowMutating,
} from '../workflowMutationGuard';

const {panelState, saveWorkflowDefinitionMock, workflowDataState} = vi.hoisted(() => ({
    panelState: {
        currentNode: undefined as Record<string, unknown> | undefined,
        setCurrentNode: vi.fn(),
        setOperationChangeInProgress: vi.fn(),
    },
    saveWorkflowDefinitionMock: vi.fn(),
    workflowDataState: {
        workflow: {definition: undefined as string | undefined, id: 'workflow-1' as string | undefined},
    },
}));

vi.mock('../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {getState: () => panelState},
}));

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: {getState: () => workflowDataState},
}));

vi.mock('../saveWorkflowDefinition', () => ({default: saveWorkflowDefinitionMock}));

function callSave() {
    saveTaskDispatcherSubtaskFieldChange({
        currentComponentDefinition: {name: 'logger', version: 1} as ComponentDefinition,
        currentNodeIndex: 0,
        fieldUpdate: {field: 'label', value: 'Renamed'},
        updateWorkflowMutation: {mutate: vi.fn()} as unknown as UpdateWorkflowMutationType,
    });
}

describe('saveTaskDispatcherSubtaskFieldChange', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        panelState.currentNode = {
            componentName: 'logger',
            conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1'},
            name: 'logger_1',
            workflowNodeName: 'logger_1',
        };

        workflowDataState.workflow = {
            definition: JSON.stringify({tasks: [{name: 'condition_1', type: 'condition/v1'}]}),
            id: 'workflow-1',
        };
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('saves nothing when no node is focused', () => {
        panelState.currentNode = undefined;

        callSave();

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(hasPendingSaves('workflow-1')).toBe(false);
    });

    it('queues a re-run of itself while another save is in flight', () => {
        // The task list it hands to saveWorkflowDefinition is precomputed from the definition read at call
        // time, so it must be rebuilt at drain time rather than replayed.
        setWorkflowMutating('workflow-1', true);

        callSave();

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(hasPendingSaves('workflow-1')).toBe(true);

        setWorkflowMutating('workflow-1', false);

        drainPendingSaves('workflow-1');

        expect(hasPendingSaves('workflow-1')).toBe(false);
    });

    it('saves straight through when no other save holds the guard', () => {
        callSave();

        expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();
        expect(hasPendingSaves('workflow-1')).toBe(false);
    });
});
