import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {toggleNodeDisabled} from './toggleNodeDisabled';
import {
    clearAllWorkflowMutations,
    drainPendingSaves,
    enqueuePendingSave,
    setWorkflowMutating,
} from './workflowMutationGuard';

function task(overrides: Partial<WorkflowTask> & {name: string}): WorkflowTask {
    return {
        parameters: {},
        type: 'test/v1/action',
        ...overrides,
    };
}

function makeDefinition(tasks: Array<WorkflowTask>): string {
    return JSON.stringify(
        {
            label: 'Test Workflow',
            tasks,
        },
        null,
        SPACE
    );
}

describe('toggleNodeDisabled', () => {
    const workflowId = 'workflow_1';

    let mutateMock: ReturnType<typeof vi.fn>;
    let queryClient: QueryClient;
    let updateWorkflowMutation: UpdateWorkflowMutationType;

    beforeEach(() => {
        clearAllWorkflowMutations();

        useWorkflowDataStore.setState({nodes: []});
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});

        mutateMock = vi.fn();
        queryClient = new QueryClient();
        updateWorkflowMutation = {mutate: mutateMock} as unknown as UpdateWorkflowMutationType;
    });

    function setDefinition(tasks: Array<WorkflowTask>) {
        useWorkflowDataStore.setState((state) => ({
            workflow: {
                ...state.workflow,
                definition: makeDefinition(tasks),
                id: workflowId,
                version: 3,
            },
        }));
    }

    it('should write disabled: true into the parsed definition passed to the mutation for a top-level task', () => {
        setDefinition([task({name: 'action_1'})]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(mutateMock).toHaveBeenCalledTimes(1);

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedTask = JSON.parse(mutatedDefinition).tasks[0];

        expect(mutatedTask.disabled).toBe(true);
    });

    it('should delete the disabled key entirely (never write false) when toggling an already-disabled task again', () => {
        setDefinition([task({disabled: true, name: 'action_1'})]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedTask = JSON.parse(mutatedDefinition).tasks[0];

        expect('disabled' in mutatedTask).toBe(false);
        expect(mutatedDefinition).not.toContain('false');
    });

    it('should find and flip a task nested in parameters.caseTrue', () => {
        const nested = task({name: 'nested_action'});
        const conditionTask = task({
            name: 'condition_1',
            parameters: {caseFalse: [], caseTrue: [nested]},
            type: 'condition/v1',
        });

        setDefinition([conditionTask]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'nested_action'});

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedNested = JSON.parse(mutatedDefinition).tasks[0].parameters.caseTrue[0];

        expect(mutatedNested.disabled).toBe(true);
    });

    it('should find and flip a task nested in parameters.cases[0].tasks', () => {
        const nested = task({name: 'branch_action'});
        const branchTask = task({
            name: 'branch_1',
            parameters: {cases: [{key: 'case_a', tasks: [nested]}]},
            type: 'branch/v1',
        });

        setDefinition([branchTask]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'branch_action'});

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedNested = JSON.parse(mutatedDefinition).tasks[0].parameters.cases[0].tasks[0];

        expect(mutatedNested.disabled).toBe(true);
    });

    it('should leave the store definition unchanged and not fire a mutation for an unknown node name', () => {
        setDefinition([task({name: 'action_1'})]);

        const definitionBeforeToggle = useWorkflowDataStore.getState().workflow.definition;

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'does_not_exist'});

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionBeforeToggle);
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('should find and flip a task nested in a single-map subtask (each.iteratee)', () => {
        const nested = task({name: 'iteratee_action'});
        const eachTask = task({
            name: 'each_1',
            parameters: {iteratee: nested},
            type: 'each/v1',
        });

        setDefinition([eachTask]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'iteratee_action'});

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedNested = JSON.parse(mutatedDefinition).tasks[0].parameters.iteratee;

        expect(mutatedNested.disabled).toBe(true);
    });

    it('should find and flip a task nested in pre/post/finalize', () => {
        const nestedInPost = task({name: 'post_action'});
        const parentTask = {
            ...task({name: 'parent_1'}),
            post: [nestedInPost],
        };

        setDefinition([parentTask]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'post_action'});

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedNested = JSON.parse(mutatedDefinition).tasks[0].post[0];

        expect(mutatedNested.disabled).toBe(true);
    });

    it('should not mistake a task-shaped user data object (e.g. a data table column) for a task', () => {
        const columnDefinition = {name: 'ime', type: 'STRING'};
        const parentTask = task({
            name: 'action_1',
            parameters: {columns: [columnDefinition]},
        });

        setDefinition([parentTask]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'ime'});

        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('should patch workflow.tasks so the store no longer disagrees with the definition', () => {
        setDefinition([task({name: 'action_1'}), task({name: 'action_2'})]);

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, tasks: [task({name: 'action_1'}), task({name: 'action_2'})]},
        }));

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        const storeTasks = useWorkflowDataStore.getState().workflow.tasks!;

        expect(storeTasks[0].disabled).toBe(true);
        expect(storeTasks[1].disabled).toBeUndefined();
    });

    it('should delete the disabled key from workflow.tasks (never write false) when re-enabling', () => {
        setDefinition([task({disabled: true, name: 'action_1'})]);

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, tasks: [task({disabled: true, name: 'action_1'})]},
        }));

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect('disabled' in useWorkflowDataStore.getState().workflow.tasks![0]).toBe(false);
    });

    it('should patch the matching React Flow node data so the badge and the menu label agree', () => {
        setDefinition([task({name: 'action_1'})]);

        useWorkflowDataStore.setState({
            nodes: [
                {data: {workflowNodeName: 'action_1'}, id: 'action_1', position: {x: 0, y: 0}},
                {data: {workflowNodeName: 'action_2'}, id: 'action_2', position: {x: 0, y: 0}},
            ],
        });

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        const storeNodes = useWorkflowDataStore.getState().nodes;

        expect(storeNodes[0].data.disabled).toBe(true);
        expect(storeNodes[1].data.disabled).toBeUndefined();
    });

    it('should patch the details panel currentNode when it is the toggled node', () => {
        setDefinition([task({name: 'action_1'})]);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'action_1', workflowNodeName: 'action_1'} as never,
        });

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.disabled).toBe(true);
    });

    it('should leave the details panel currentNode untouched when a different node is toggled', () => {
        setDefinition([task({name: 'action_1'}), task({name: 'action_2'})]);

        const otherNode = {name: 'action_2', workflowNodeName: 'action_2'} as never;

        useWorkflowNodeDetailsPanelStore.setState({currentNode: otherNode});

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBe(otherNode);
    });

    it('should keep the disabled flag through a later decorative panel save of the same node', async () => {
        setDefinition([task({name: 'action_1'})]);

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, tasks: [task({name: 'action_1'})]},
        }));

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {
                componentName: 'test',
                name: 'action_1',
                operationName: 'action',
                parameters: {},
                type: 'test/v1/action',
                version: 1,
                workflowNodeName: 'action_1',
            } as never,
        });

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        setWorkflowMutating(workflowId, false);

        const currentNode = useWorkflowNodeDetailsPanelStore.getState().currentNode!;

        await saveWorkflowDefinition({
            decorative: true,
            nodeData: {...currentNode, label: 'Renamed'} as never,
            updateWorkflowMutation,
        });

        const savedDefinition = mutateMock.mock.calls.at(-1)![0].workflow.definition;
        const savedTask = JSON.parse(savedDefinition).tasks[0];

        expect(savedTask.label).toBe('Renamed');
        expect(savedTask.disabled).toBe(true);
    });

    it('should queue the whole toggle instead of firing while another mutation is in flight', () => {
        setDefinition([task({name: 'action_1'})]);
        setWorkflowMutating(workflowId, true);

        const definitionBeforeToggle = useWorkflowDataStore.getState().workflow.definition;

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(mutateMock).not.toHaveBeenCalled();
        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionBeforeToggle);

        setWorkflowMutating(workflowId, false);
        drainPendingSaves(workflowId);

        expect(mutateMock).toHaveBeenCalledTimes(1);
        expect(JSON.parse(mutateMock.mock.calls[0][0].workflow.definition).tasks[0].disabled).toBe(true);
    });

    it('should apply a toggle queued behind another save to the definition that save produced', () => {
        setDefinition([task({name: 'action_1'})]);
        setWorkflowMutating(workflowId, true);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        setDefinition([task({label: 'Renamed', name: 'action_1'})]);
        setWorkflowMutating(workflowId, false);
        drainPendingSaves(workflowId);

        const savedTask = JSON.parse(mutateMock.mock.calls[0][0].workflow.definition).tasks[0];

        expect(savedTask.label).toBe('Renamed');
        expect(savedTask.disabled).toBe(true);
    });

    it('should run saves queued during the toggle once the toggle settles', () => {
        setDefinition([task({name: 'action_1'})]);

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        const queuedSave = vi.fn();

        enqueuePendingSave(workflowId, queuedSave);

        expect(queuedSave).not.toHaveBeenCalled();

        mutateMock.mock.calls[0][1].onSettled();

        expect(queuedSave).toHaveBeenCalledTimes(1);
    });

    it('should restore the previous definition when the toggle save fails', () => {
        setDefinition([task({name: 'action_1'})]);

        const definitionBeforeToggle = useWorkflowDataStore.getState().workflow.definition;

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(useWorkflowDataStore.getState().workflow.definition).not.toBe(definitionBeforeToggle);

        mutateMock.mock.calls[0][1].onError();

        expect(useWorkflowDataStore.getState().workflow.definition).toBe(definitionBeforeToggle);
    });

    it('should invalidate the workflow node outputs once the toggle is saved', () => {
        setDefinition([task({name: 'action_1'})]);

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        toggleNodeDisabled({queryClient, updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(invalidateQueriesSpy).not.toHaveBeenCalled();

        mutateMock.mock.calls[0][1].onSuccess({version: 4});

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['workflowNodeOutputs', workflowId]});
    });
});
