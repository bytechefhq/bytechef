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
    setWorkflowWithoutHistory: (workflow: unknown) => mockWorkflowState.setWorkflow(workflow),
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
        it('queues the save while another mutation is in flight and fires it from fresh state once released', async () => {
            mockWorkflowState = makeWorkflowState();

            const mutation = makeMutation();

            const {drainPendingSaves, hasPendingSaves, setWorkflowMutating} = await import('./workflowMutationGuard');

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
            expect(mockWorkflowState.setWorkflow).not.toHaveBeenCalled();
            expect(hasPendingSaves('workflow-1')).toBe(true);

            mockWorkflowState = makeWorkflowState([{name: 'email_1', type: 'email/v1/send'}]);

            setWorkflowMutating('workflow-1', false);

            drainPendingSaves('workflow-1');

            expect(mutation.mutate).toHaveBeenCalledTimes(1);

            const savedDefinition = JSON.parse(
                (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0].workflow.definition
            );

            expect(savedDefinition.tasks.map((task: {name: string}) => task.name)).toEqual(['email_1', 'httpClient_1']);
            expect(hasPendingSaves('workflow-1')).toBe(false);
            expect(isWorkflowMutating('workflow-1')).toBe(true);
        });

        it('drains the next queued save when its own mutation settles', async () => {
            mockWorkflowState = makeWorkflowState();

            const mutation = makeMutation();

            const {setWorkflowMutating} = await import('./workflowMutationGuard');

            await saveWorkflowDefinition({
                nodeData: {componentName: 'httpClient', name: 'httpClient_1', operationName: 'get', version: 1},
                updateWorkflowMutation: mutation,
            } as unknown as Parameters<typeof saveWorkflowDefinition>[0]);

            expect(isWorkflowMutating('workflow-1')).toBe(true);

            await saveWorkflowDefinition({
                nodeData: {componentName: 'logger', name: 'logger_1', operationName: 'debug', version: 1},
                updateWorkflowMutation: mutation,
            } as unknown as Parameters<typeof saveWorkflowDefinition>[0]);

            expect(mutation.mutate).toHaveBeenCalledTimes(1);

            const firstCallbacks = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][1];

            firstCallbacks.onSettled();

            expect(mutation.mutate).toHaveBeenCalledTimes(2);

            const secondDefinition = JSON.parse(
                (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[1][0].workflow.definition
            );

            expect(secondDefinition.tasks.map((task: {name: string}) => task.name)).toContain('logger_1');
            expect(isWorkflowMutating('workflow-1')).toBe(true);

            setWorkflowMutating('workflow-1', false);
        });

        it('drops a precomputed-tasks save while another mutation is in flight instead of replaying stale tasks', async () => {
            mockWorkflowState = makeWorkflowState();

            const mutation = makeMutation();

            const {hasPendingSaves, setWorkflowMutating} = await import('./workflowMutationGuard');

            setWorkflowMutating('workflow-1', true);

            await saveWorkflowDefinition({
                updateWorkflowMutation: mutation,
                updatedWorkflowTasks: [{name: 'stale_1', type: 'logger/v1/debug'}],
            });

            expect(mutation.mutate).not.toHaveBeenCalled();
            expect(hasPendingSaves('workflow-1')).toBe(false);
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
        it('should strip array shaped connections left in the definition by earlier client versions', async () => {
            mockWorkflowState = makeWorkflowState([
                {
                    name: 'condition_1',
                    parameters: {
                        caseFalse: [],
                        caseTrue: [
                            {
                                connections: [],
                                finalize: [],
                                name: 'condition_2',
                                parameters: {caseFalse: [], caseTrue: []},
                                post: [],
                                pre: [],
                                type: 'condition/v1',
                            },
                        ],
                    },
                    type: 'condition/v1',
                },
            ]);

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

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];

            const savedDefinition = JSON.parse(mutateArgs.workflow.definition);
            const savedConditionTask = savedDefinition.tasks.find(
                (task: {name: string}) => task.name === 'condition_1'
            );
            const savedNestedTask = savedConditionTask.parameters.caseTrue[0];

            expect(savedNestedTask.name).toBe('condition_2');
            expect(savedNestedTask).not.toHaveProperty('connections');
        });

        it('should convert definition connections maps into DTO connections arrays', async () => {
            mockWorkflowState = makeWorkflowState([
                {
                    connections: {javascript01: {componentName: 'googleMail', componentVersion: 1}},
                    name: 'script_1',
                    parameters: {},
                    type: 'script/v1/javascript',
                },
            ]);

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

            const scriptTask = optimisticWorkflow.tasks.find((task: {name: string}) => task.name === 'script_1');

            expect(scriptTask.connections).toEqual([
                {
                    componentName: 'googleMail',
                    componentVersion: 1,
                    key: 'javascript01',
                    required: false,
                    workflowNodeName: 'script_1',
                },
            ]);
        });

        it('should keep the definition connections map in the saved definition', async () => {
            mockWorkflowState = makeWorkflowState([
                {
                    connections: {javascript01: {componentName: 'googleMail', componentVersion: 1}},
                    name: 'script_1',
                    parameters: {},
                    type: 'script/v1/javascript',
                },
            ]);

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

            const mutateArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];
            const savedDefinition = JSON.parse(mutateArgs.workflow.definition);

            const scriptTask = savedDefinition.tasks.find((task: {name: string}) => task.name === 'script_1');

            expect(scriptTask.connections).toEqual({
                javascript01: {componentName: 'googleMail', componentVersion: 1},
            });
        });

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

        it('calls onError after rolling back so the caller can undo what it did optimistically', async () => {
            mockWorkflowState = makeWorkflowState();
            const mutation = makeMutation();
            const onError = vi.fn();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'httpClient',
                    name: 'httpClient_1',
                    operationName: 'get',
                    version: 1,
                } as unknown as NodeDataType,
                onError,
                updateWorkflowMutation: mutation,
            });

            const callbacks = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][1];

            callbacks.onError(new Error('version conflict'));

            expect(onError).toHaveBeenCalledOnce();
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

    describe('clusterRoot preservation in optimistic update', () => {
        it('should preserve clusterRoot on existing tasks when adding a new node', async () => {
            mockWorkflowState = makeWorkflowState([
                {
                    clusterElements: {source: [{name: 'csv_1', type: 'csvFile/v1/read'}]},
                    clusterRoot: true,
                    name: 'dataStream_1',
                    parameters: {},
                    type: 'dataStream/v1/stream',
                },
            ]);
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

            expect(optimisticWorkflow.tasks).toHaveLength(2);
            expect(optimisticWorkflow.tasks[0].name).toBe('dataStream_1');
            expect(optimisticWorkflow.tasks[0].clusterRoot).toBe(true);
            expect(optimisticWorkflow.tasks[1].name).toBe('httpClient_1');
        });

        it('should not add clusterRoot to tasks that did not have it', async () => {
            mockWorkflowState = makeWorkflowState([{name: 'logger_1', parameters: {}, type: 'logger/v1/info'}]);
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

            expect(optimisticWorkflow.tasks).toHaveLength(2);
            expect(optimisticWorkflow.tasks[0].clusterRoot).toBeUndefined();
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
    describe('clusterRoot clusterElements preservation', () => {
        const clusterElements = {
            model: {label: 'Claude', name: 'anthropic_1', parameters: {}, type: 'anthropic/v1/model'},
            tools: [{label: 'Sheets', name: 'googleSheets_1', parameters: {}, type: 'googleSheets/v1/insertRow'}],
        };

        function makeClusterRootState() {
            return makeWorkflowState([
                {
                    clusterElements,
                    label: 'Agent',
                    name: 'aiAgent_1',
                    parameters: {systemPrompt: 'Be helpful'},
                    type: 'aiAgent/v1/chat',
                },
            ]);
        }

        beforeEach(() => {
            mockWorkflowState = makeClusterRootState();

            mockWorkflowState.workflow.tasks = mockWorkflowState.workflow.tasks.map((task) => ({
                ...task,
                clusterRoot: true,
            }));
        });

        it('should keep the existing cluster elements when the node data does not carry them', async () => {
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    componentName: 'aiAgent',
                    name: 'aiAgent_1',
                    parameters: {systemPrompt: 'Be helpful'},
                    type: 'aiAgent/v1/streamChat',
                    workflowNodeName: 'aiAgent_1',
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            const savedTask = JSON.parse(
                (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0].workflow.definition
            ).tasks[0];

            expect(savedTask.clusterElements).toEqual(clusterElements);
        });

        it('should still clear the cluster elements when the node data carries an explicit empty map', async () => {
            const mutation = makeMutation();

            await saveWorkflowDefinition({
                nodeData: {
                    clusterElements: {},
                    componentName: 'aiAgent',
                    name: 'aiAgent_1',
                    parameters: {systemPrompt: 'Be helpful'},
                    type: 'aiAgent/v1/chat',
                    workflowNodeName: 'aiAgent_1',
                } as unknown as NodeDataType,
                updateWorkflowMutation: mutation,
            });

            const savedTask = JSON.parse(
                (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0].workflow.definition
            ).tasks[0];

            expect(savedTask.clusterElements).toEqual({});
        });
    });
});
