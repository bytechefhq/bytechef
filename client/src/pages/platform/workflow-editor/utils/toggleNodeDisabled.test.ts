import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {toggleNodeDisabled} from './toggleNodeDisabled';
import {clearAllWorkflowMutations, consumePendingDefinition, setWorkflowMutating} from './workflowMutationGuard';

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
    let updateWorkflowMutation: UpdateWorkflowMutationType;

    beforeEach(() => {
        clearAllWorkflowMutations();

        useWorkflowDataStore.setState({nodes: []});
        useWorkflowNodeDetailsPanelStore.setState({currentNode: undefined});

        mutateMock = vi.fn();
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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(mutateMock).toHaveBeenCalledTimes(1);

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedTask = JSON.parse(mutatedDefinition).tasks[0];

        expect(mutatedTask.disabled).toBe(true);
    });

    it('should delete the disabled key entirely (never write false) when toggling an already-disabled task again', () => {
        setDefinition([task({disabled: true, name: 'action_1'})]);

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'nested_action'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'branch_action'});

        const mutatedDefinition = mutateMock.mock.calls[0][0].workflow.definition;
        const mutatedNested = JSON.parse(mutatedDefinition).tasks[0].parameters.cases[0].tasks[0];

        expect(mutatedNested.disabled).toBe(true);
    });

    it('should leave the store definition unchanged and not fire a mutation for an unknown node name', () => {
        setDefinition([task({name: 'action_1'})]);

        const definitionBeforeToggle = useWorkflowDataStore.getState().workflow.definition;

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'does_not_exist'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'iteratee_action'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'post_action'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'ime'});

        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('should patch workflow.tasks so the store no longer disagrees with the definition', () => {
        setDefinition([task({name: 'action_1'}), task({name: 'action_2'})]);

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, tasks: [task({name: 'action_1'}), task({name: 'action_2'})]},
        }));

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

        const storeTasks = useWorkflowDataStore.getState().workflow.tasks!;

        expect(storeTasks[0].disabled).toBe(true);
        expect(storeTasks[1].disabled).toBeUndefined();
    });

    it('should delete the disabled key from workflow.tasks (never write false) when re-enabling', () => {
        setDefinition([task({disabled: true, name: 'action_1'})]);

        useWorkflowDataStore.setState((state) => ({
            workflow: {...state.workflow, tasks: [task({disabled: true, name: 'action_1'})]},
        }));

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

        const storeNodes = useWorkflowDataStore.getState().nodes;

        expect(storeNodes[0].data.disabled).toBe(true);
        expect(storeNodes[1].data.disabled).toBeUndefined();
    });

    it('should patch the details panel currentNode when it is the toggled node', () => {
        setDefinition([task({name: 'action_1'})]);

        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'action_1', workflowNodeName: 'action_1'} as never,
        });

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode?.disabled).toBe(true);
    });

    it('should leave the details panel currentNode untouched when a different node is toggled', () => {
        setDefinition([task({name: 'action_1'}), task({name: 'action_2'})]);

        const otherNode = {name: 'action_2', workflowNodeName: 'action_2'} as never;

        useWorkflowNodeDetailsPanelStore.setState({currentNode: otherNode});

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

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

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

        // The toggle mutation never settles in this test, so release the guard by hand
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

    it('should queue the definition instead of firing while another mutation is in flight', () => {
        setDefinition([task({name: 'action_1'})]);
        setWorkflowMutating(workflowId, true);

        toggleNodeDisabled({updateWorkflowMutation, workflowNodeName: 'action_1'});

        expect(mutateMock).not.toHaveBeenCalled();

        const pendingDefinition = consumePendingDefinition(workflowId);

        expect(pendingDefinition).toBe(useWorkflowDataStore.getState().workflow.definition);
        expect(JSON.parse(pendingDefinition!).tasks[0].disabled).toBe(true);
    });
});
