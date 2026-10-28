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

function callSave(onError?: () => void) {
    saveTaskDispatcherSubtaskFieldChange({
        currentComponentDefinition: {name: 'logger', version: 1} as ComponentDefinition,
        currentNodeIndex: 0,
        fieldUpdate: {field: 'label', value: 'Renamed'},
        onError,
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
            definition: JSON.stringify({
                tasks: [
                    {
                        name: 'condition_1',
                        parameters: {caseFalse: [], caseTrue: [{name: 'logger_1', type: 'logger/v1/info'}]},
                        type: 'condition/v1',
                    },
                ],
            }),
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

    it('reports a failed save to the caller', () => {
        const onError = vi.fn();

        callSave(onError);

        saveWorkflowDefinitionMock.mock.calls[0][0].onError();

        expect(onError).toHaveBeenCalledOnce();
    });

    it('reports a save it cannot start to the caller', () => {
        const onError = vi.fn();

        workflowDataState.workflow = {definition: undefined, id: 'workflow-1'};

        callSave(onError);

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
    });

    it('reports a save for a node outside any task dispatcher to the caller', () => {
        const onError = vi.fn();

        panelState.currentNode = {componentName: 'logger', name: 'logger_1', workflowNodeName: 'logger_1'};

        callSave(onError);

        expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
        expect(onError).toHaveBeenCalledOnce();
    });

    describe('in a branch case', () => {
        beforeEach(() => {
            workflowDataState.workflow = {
                definition: JSON.stringify({
                    tasks: [
                        {
                            name: 'branch_1',
                            parameters: {
                                cases: [
                                    {key: 'ON', tasks: [{label: 'First', name: 'logger_1', type: 'logger/v1/info'}]},
                                    {key: 'OFF', tasks: [{label: 'Second', name: 'logger_2', type: 'logger/v1/info'}]},
                                ],
                                default: [],
                            },
                            type: 'branch/v1',
                        },
                    ],
                }),
                id: 'workflow-1',
            };
        });

        it('saves the new label into the case that holds the node', () => {
            panelState.currentNode = {
                branchData: {branchId: 'branch_1', caseKey: 'OFF', index: 0},
                componentName: 'logger',
                name: 'logger_2',
                workflowNodeName: 'logger_2',
            };

            callSave();

            expect(saveWorkflowDefinitionMock).toHaveBeenCalledOnce();

            const [branchTask] = saveWorkflowDefinitionMock.mock.calls[0][0].updatedWorkflowTasks;

            expect(branchTask.parameters.cases).toEqual([
                {key: 'ON', tasks: [{label: 'First', name: 'logger_1', type: 'logger/v1/info'}]},
                {key: 'OFF', tasks: [{label: 'Renamed', name: 'logger_2', type: 'logger/v1/info'}]},
            ]);
        });

        it('saves nothing when the case key it holds no longer names the case that holds the node', () => {
            // A case renamed on the canvas while the node's panel stayed open used to make this save append an
            // empty case under the old key and drop the label edit.
            const onError = vi.fn();

            panelState.currentNode = {
                branchData: {branchId: 'branch_1', caseKey: 'RENAMED_AWAY', index: 0},
                componentName: 'logger',
                name: 'logger_2',
                workflowNodeName: 'logger_2',
            };

            callSave(onError);

            expect(saveWorkflowDefinitionMock).not.toHaveBeenCalled();
            expect(onError).toHaveBeenCalledOnce();
        });
    });

    it('does not report a save queued behind another save as failed', () => {
        const onError = vi.fn();

        setWorkflowMutating('workflow-1', true);

        callSave(onError);

        expect(onError).not.toHaveBeenCalled();
    });
});
