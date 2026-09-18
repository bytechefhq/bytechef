import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../../stores/useWorkflowNodeDetailsPanelStore';
import handleDeleteTrigger from '../handleDeleteTrigger';
import {isWorkflowMutating, setWorkflowMutating} from '../workflowMutationGuard';

vi.mock('../../utils/workflowMutationGuard', () => ({
    isWorkflowMutating: vi.fn().mockReturnValue(false),
    setWorkflowMutating: vi.fn(),
}));

const makeTrigger = (name: string): WorkflowTrigger => ({
    name,
    type: `manual/v1/${name}`,
});

const makeWorkflow = (triggers: WorkflowTrigger[]) => ({
    definition: JSON.stringify({tasks: [], triggers}),
    id: 'workflow-1',
    nodeNames: [],
    triggers,
    version: 1,
});

describe('handleDeleteTrigger', () => {
    const cancelWorkflowQueries = vi.fn();
    const invalidateWorkflowQueries = vi.fn();
    const mutateMock = vi.fn();

    const updateWorkflowMutation = {
        mutate: mutateMock,
    } as unknown as Parameters<typeof handleDeleteTrigger>[0]['updateWorkflowMutation'];

    beforeEach(() => {
        vi.clearAllMocks();

        useWorkflowDataStore.setState(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            {workflow: makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')])} as any
        );

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: undefined,
            workflowNodeDetailsPanelOpen: false,
        });
    });

    it('calls updateWorkflowMutation.mutate with definition that contains only the sibling trigger', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]);

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow: workflow as ReturnType<typeof makeWorkflow>,
        });

        expect(mutateMock).toHaveBeenCalledOnce();

        const mutateArg = mutateMock.mock.calls[0][0] as {workflow: {definition: string}};
        const parsedDefinition = JSON.parse(mutateArg.workflow.definition) as {triggers: WorkflowTrigger[]};

        expect(parsedDefinition.triggers).toHaveLength(1);
        expect(parsedDefinition.triggers[0].name).toBe('trigger_2');
    });

    it('does NOT delete when only one trigger remains', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1')]);

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow: workflow as ReturnType<typeof makeWorkflow>,
        });

        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('closes the details panel when the deleted trigger is currently open', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]);

        useWorkflowNodeDetailsPanelStore.setState(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            {currentNode: {name: 'trigger_1', trigger: true}, workflowNodeDetailsPanelOpen: true} as any
        );

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow: workflow as ReturnType<typeof makeWorkflow>,
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(false);
    });

    it('preserves the details panel when a different trigger is deleted', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]);

        useWorkflowNodeDetailsPanelStore.setState(
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            {currentNode: {name: 'trigger_2', trigger: true}, workflowNodeDetailsPanelOpen: true} as any
        );

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow: workflow as ReturnType<typeof makeWorkflow>,
        });

        expect(useWorkflowNodeDetailsPanelStore.getState().workflowNodeDetailsPanelOpen).toBe(true);
    });

    it('does nothing when the workflow has no definition', () => {
        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow: {...makeWorkflow([]), definition: undefined} as unknown as ReturnType<typeof makeWorkflow>,
        });

        expect(cancelWorkflowQueries).not.toHaveBeenCalled();
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('does nothing while another save of the same workflow is in flight', () => {
        vi.mocked(isWorkflowMutating).mockReturnValueOnce(true);

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow: makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]),
        });

        expect(cancelWorkflowQueries).not.toHaveBeenCalled();
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('restores the previous workflow and refetches when the save fails', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]);

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow,
        });

        const {onError} = mutateMock.mock.calls[0][1] as {onError: () => void};

        onError();

        expect(useWorkflowDataStore.getState().workflow.triggers?.map((trigger) => trigger.name)).toEqual([
            'trigger_1',
            'trigger_2',
        ]);
        expect(invalidateWorkflowQueries).toHaveBeenCalledOnce();
    });

    it('releases the mutation guard and refetches once the save settles', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]);

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow,
        });

        expect(setWorkflowMutating).toHaveBeenLastCalledWith('workflow-1', true);

        const {onSettled} = mutateMock.mock.calls[0][1] as {onSettled: () => void};

        onSettled();

        expect(setWorkflowMutating).toHaveBeenLastCalledWith('workflow-1', false);
        expect(invalidateWorkflowQueries).toHaveBeenCalledOnce();
    });

    it('keeps the updated triggers and takes the saved version on success', () => {
        const workflow = makeWorkflow([makeTrigger('trigger_1'), makeTrigger('trigger_2')]);

        handleDeleteTrigger({
            cancelWorkflowQueries,
            invalidateWorkflowQueries,
            triggerName: 'trigger_1',
            updateWorkflowMutation,
            workflow,
        });

        const {onSuccess} = mutateMock.mock.calls[0][1] as {onSuccess: (updatedWorkflow: {version: number}) => void};

        onSuccess({version: 2});

        const storedWorkflow = useWorkflowDataStore.getState().workflow;

        expect(storedWorkflow.version).toBe(2);
        expect(storedWorkflow.triggers?.map((trigger) => trigger.name)).toEqual(['trigger_2']);
    });
});
