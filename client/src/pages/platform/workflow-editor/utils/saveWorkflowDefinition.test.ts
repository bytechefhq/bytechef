import {SPACE} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import saveWorkflowDefinition from './saveWorkflowDefinition';
import {clearAllWorkflowMutations, isWorkflowMutating} from './workflowMutationGuard';

// ── Store mocks ──────────────────────────────────────────────────────

let mockWorkflowState: ReturnType<typeof makeWorkflowState>;

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: {
        getState: () => mockWorkflowState,
    },
}));

vi.mock('../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {
        getState: () => ({
            reset: vi.fn(),
        }),
    },
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore', () => ({
    default: {
        getState: () => ({
            setWorkflowTestChatPanelOpen: vi.fn(),
        }),
    },
}));

// ── Helpers ──────────────────────────────────────────────────────────

function makeWorkflowState(tasks: Array<Record<string, unknown>> = [], triggers: Array<Record<string, unknown>> = []) {
    return {
        setWorkflow: vi.fn(),
        workflow: {
            definition: JSON.stringify({tasks, triggers}, null, SPACE),
            id: 'workflow-1',
            tasks: tasks.map((task) => ({...task})),
            version: 1,
        },
    };
}

function makeMutation() {
    return {
        mutate: vi.fn(),
    } as unknown as Parameters<typeof saveWorkflowDefinition>[0]['updateWorkflowMutation'];
}

// ── Tests ────────────────────────────────────────────────────────────

describe('saveWorkflowDefinition', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockWorkflowState = makeWorkflowState();
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    describe('trigger save', () => {
        it('should save a trigger with explicit type', async () => {
            mockWorkflowState = makeWorkflowState([], [{name: 'manual', type: 'manual/v1'}]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'webhook',
                    name: 'webhook_trigger',
                    operationName: 'onReceive',
                    parameters: {path: '/hook'},
                    trigger: true,
                    type: 'webhook/v1/onReceive',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.triggers).toHaveLength(1);
            expect(updatedDefinition.triggers[0].name).toBe('webhook_trigger');
            expect(updatedDefinition.triggers[0].type).toBe('webhook/v1/onReceive');
            expect(updatedDefinition.triggers[0].parameters).toEqual({path: '/hook'});
        });

        it('should construct type from componentName/version/operationName when type is not provided', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'schedule',
                    name: 'schedule_trigger',
                    operationName: 'onInterval',
                    trigger: true,
                    version: 2,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.triggers[0].type).toBe('schedule/v2/onInterval');
        });

        it('should call onSuccess after saving a trigger', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();
            const onSuccess = vi.fn();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'manual',
                    name: 'manual_trigger',
                    operationName: 'trigger',
                    trigger: true,
                    type: 'manual/v1/trigger',
                    version: 1,
                } as unknown as NodeDataType,
                onSuccess,
                updateWorkflowMutation: mutation,
            });

            // Simulate the mutation's onSuccess callback
            const callbacks = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][1];

            callbacks.onSuccess();

            expect(onSuccess).toHaveBeenCalledOnce();
        });
    });

    describe('new task creation', () => {
        it('should append a new task when no existing task matches', async () => {
            mockWorkflowState = makeWorkflowState([{name: 'task_1', parameters: {}, type: 'test/v1/action'}]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.tasks).toHaveLength(2);
            expect(updatedDefinition.tasks[1].name).toBe('httpClient_1');
            expect(updatedDefinition.tasks[1].type).toBe('httpClient/v1/get');
        });

        it('should insert a new task at a specific index', async () => {
            mockWorkflowState = makeWorkflowState([
                {name: 'task_1', parameters: {}, type: 'test/v1/action'},
                {name: 'task_2', parameters: {}, type: 'test/v1/action'},
            ]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'post',
                    version: 1,
                } as unknown as NodeDataType,
                nodeIndex: 1,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.tasks).toHaveLength(3);
            expect(updatedDefinition.tasks[1].name).toBe('httpClient_1');
        });

        it('should construct task dispatcher type without operationName', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'condition',
                    name: 'condition_1',
                    taskDispatcher: true,
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.tasks[0].type).toBe('condition/v1');
        });
    });

    describe('existing task update', () => {
        it('should update parameters when they differ', async () => {
            mockWorkflowState = makeWorkflowState([
                {name: 'httpClient_1', parameters: {url: 'http://old.com'}, type: 'httpClient/v1/get'},
            ]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    parameters: {url: 'http://new.com'},
                    type: 'httpClient/v1/get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.tasks[0].parameters.url).toBe('http://new.com');
        });

        it('should replace parameters when type changes', async () => {
            mockWorkflowState = makeWorkflowState([
                {name: 'httpClient_1', parameters: {url: 'http://old.com'}, type: 'httpClient/v1/get'},
            ]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'post',
                    parameters: {body: '{}'},
                    type: 'httpClient/v1/post',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            // Old parameters should be replaced, not merged
            expect(updatedDefinition.tasks[0].parameters).toEqual({body: '{}'});
            expect(updatedDefinition.tasks[0].type).toBe('httpClient/v1/post');
        });

        it('should skip save when nothing changed and no operationName', async () => {
            const existingTask = {
                name: 'httpClient_1',
                parameters: {url: 'http://example.com'},
                type: 'httpClient/v1/get',
            };

            mockWorkflowState = makeWorkflowState([existingTask]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    parameters: {url: 'http://example.com'},
                    type: 'httpClient/v1/get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            // Should not call mutate when no changes detected and no operationName
            expect(mutation.mutate).not.toHaveBeenCalled();
        });
    });

    describe('mutation guard', () => {
        it('should not mutate when workflow is already mutating', async () => {
            mockWorkflowState = makeWorkflowState();

            const mutation = makeMutation();

            // Pre-set mutation flag
            const {setWorkflowMutating} = await import('./workflowMutationGuard');

            setWorkflowMutating('workflow-1', true);

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            expect(mutation.mutate).not.toHaveBeenCalled();
        });

        it('should set and clear mutation flag around mutate', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            // Flag should be set after mutate call
            expect(isWorkflowMutating('workflow-1')).toBe(true);

            // Simulate settled callback
            const callbacks = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][1];

            callbacks.onSettled();

            expect(isWorkflowMutating('workflow-1')).toBe(false);
        });
    });

    describe('with updatedWorkflowTasks', () => {
        it('should use provided tasks instead of computing them', async () => {
            mockWorkflowState = makeWorkflowState([{name: 'old_task', parameters: {}, type: 'test/v1/action'}]);
            const mutation = makeMutation();

            const customTasks = [
                {name: 'custom_1', parameters: {}, type: 'custom/v1/action'},
                {name: 'custom_2', parameters: {}, type: 'custom/v1/action'},
            ];

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'custom',
                    name: 'custom_1',
                    operationName: 'action',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
                updatedWorkflowTasks: customTasks,
            });

            expect(mutation.mutate).toHaveBeenCalledOnce();

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const updatedDefinition = JSON.parse(mutateArgs.workflow.definition);

            expect(updatedDefinition.tasks).toEqual(customTasks);
        });
    });

    describe('optimistic update', () => {
        it('should update the store with new definition before calling mutate', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            // setWorkflow should be called before mutate (optimistic update)
            expect(mockWorkflowState.setWorkflow).toHaveBeenCalledOnce();

            const optimisticWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

            expect(optimisticWorkflow.definition).toContain('httpClient_1');
        });

        it('should rollback to previous workflow on mutation error', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            // Reset the mock to track the rollback call separately
            mockWorkflowState.setWorkflow.mockClear();

            const callbacks = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][1];

            callbacks.onError(new Error('version conflict'));

            expect(mockWorkflowState.setWorkflow).toHaveBeenCalledOnce();

            const rolledBackWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

            // Rolled-back workflow should NOT contain the new task
            expect(rolledBackWorkflow.definition).not.toContain('httpClient_1');
            expect(rolledBackWorkflow.id).toBe('workflow-1');
        });

        it('should update store with server response on mutation success', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            mockWorkflowState.setWorkflow.mockClear();

            const callbacks = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][1];
            const serverResponse = {
                id: 'workflow-1',
                tasks: [{name: 'httpClient_1', parameters: {}, type: 'httpClient/v1/get'}],
                version: 5,
            };

            callbacks.onSuccess(serverResponse);

            expect(mockWorkflowState.setWorkflow).toHaveBeenCalledOnce();

            const updatedWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

            // Should use the server's version
            expect(updatedWorkflow.version).toBe(5);
            // Should use the server's tasks
            expect(updatedWorkflow.tasks).toEqual(serverResponse.tasks);
            // But should keep the local definition (not the server's)
            expect(updatedWorkflow.definition).toContain('httpClient_1');
        });
    });

    describe('optimistic task insertion', () => {
        it('should include new task in optimistic tasks when adding a node', async () => {
            mockWorkflowState = makeWorkflowState([{name: 'existing_1', parameters: {}, type: 'test/v1/action'}]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            const optimisticWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

            // Should include both existing and new task
            expect(optimisticWorkflow.tasks).toHaveLength(2);
            expect(optimisticWorkflow.tasks[0].name).toBe('existing_1');
            expect(optimisticWorkflow.tasks[1].name).toBe('httpClient_1');
        });

        it('should insert new task at specified nodeIndex', async () => {
            mockWorkflowState = makeWorkflowState([
                {name: 'task_1', parameters: {}, type: 'test/v1/action'},
                {name: 'task_2', parameters: {}, type: 'test/v1/action'},
            ]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                nodeIndex: 1,
                updateWorkflowMutation: mutation,
            });

            const optimisticWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

            expect(optimisticWorkflow.tasks).toHaveLength(3);
            expect(optimisticWorkflow.tasks[0].name).toBe('task_1');
            expect(optimisticWorkflow.tasks[1].name).toBe('httpClient_1');
            expect(optimisticWorkflow.tasks[2].name).toBe('task_2');
        });

        it('should not modify tasks when updating an existing task', async () => {
            const existingTask = {
                name: 'httpClient_1',
                parameters: {url: 'http://old.com'},
                type: 'httpClient/v1/get',
            };

            mockWorkflowState = makeWorkflowState([existingTask]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    parameters: {url: 'http://new.com'},
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            const optimisticWorkflow = mockWorkflowState.setWorkflow.mock.calls[0][0];

            // Should keep the existing tasks unchanged (no new task inserted)
            expect(optimisticWorkflow.tasks).toHaveLength(1);
            expect(optimisticWorkflow.tasks[0].name).toBe('httpClient_1');
        });
    });

    describe('decorative flag', () => {
        it('should save when decorative is true even without other changes', async () => {
            const existingTask = {
                name: 'httpClient_1',
                parameters: {url: 'http://example.com'},
                type: 'httpClient/v1/get',
            };

            mockWorkflowState = makeWorkflowState([existingTask]);
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                decorative: true,
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    parameters: {url: 'http://example.com'},
                    type: 'httpClient/v1/get',
                    version: 1,
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            // Decorative forces the save even when no substantive changes
            expect(mutation.mutate).toHaveBeenCalledOnce();
        });
    });
});
