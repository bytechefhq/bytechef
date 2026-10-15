import {SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import handleDeleteTask from '../handleDeleteTask';
import {clearAllWorkflowMutations} from '../workflowMutationGuard';

const {resetDetailsPanelMock} = vi.hoisted(() => ({resetDetailsPanelMock: vi.fn()}));

let mockWorkflowState: Record<string, unknown> = {};
const mockSetWorkflow = vi.fn((newWorkflow) => {
    mockWorkflowState = newWorkflow;
});

vi.mock('../../stores/useWorkflowDataStore', () => ({
    default: {
        getState: () => ({
            setWorkflow: mockSetWorkflow,
            workflow: mockWorkflowState,
        }),
    },
    setWorkflowWithoutHistory: (workflow: unknown) => mockSetWorkflow(workflow),
}));

vi.mock('../../stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: {
        getState: () => ({
            removePendingSaveNodeName: vi.fn(),
            reset: resetDetailsPanelMock,
            setWorkflowNodeDetailsPanelOpen: vi.fn(),
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

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    invalidatePreviousWorkflowNodeOutputsForWorkflow: vi.fn(),
}));

function makeWorkflow(tasks: WorkflowTask[]) {
    const definition = JSON.stringify({tasks}, null, SPACE);

    return {
        definition,
        id: 'workflow-1',
        nodeNames: tasks.map((task) => task.name),
        tasks,
        version: 1,
    };
}

function makeMockMutation() {
    return {
        mutate: vi.fn(),
    } as unknown as Parameters<typeof handleDeleteTask>[0]['updateWorkflowMutation'];
}

function makeQueryClient() {
    return {
        invalidateQueries: vi.fn(),
    } as unknown as QueryClient;
}

function getUpdatedTasks(mutation: ReturnType<typeof makeMockMutation>) {
    const mutationArgs = (mutation.mutate as ReturnType<typeof vi.fn>).mock.calls[0][0];

    return JSON.parse(mutationArgs.workflow.definition).tasks;
}

describe('handleDeleteTask cluster-element routing', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockWorkflowState = {};
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('deletes a cluster-root task itself via the plain top-level path, not the cluster branch', () => {
        const rootTask = {
            clusterElements: {tools: []},
            clusterRoot: true,
            name: 'aiAgent_1',
            type: 'aiAgent/v1/AGENT',
        } as unknown as WorkflowTask;
        const otherTask = {name: 'task_1', type: 'test/task_1'} as WorkflowTask;
        const workflow = makeWorkflow([rootTask, otherTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            data: {
                clusterRoot: true,
                componentName: 'aiAgent',
                name: 'aiAgent_1',
                workflowNodeName: 'aiAgent_1',
            } as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const updatedTasks = getUpdatedTasks(mutation);

        expect(updatedTasks.map((task: WorkflowTask) => task.name)).toEqual(['task_1']);
    });

    it('deletes a first-level (depth-1) cluster element, keeping the root task', () => {
        const rootTask = {
            clusterElements: {
                tools: [
                    {name: 'tool_1', type: 'tool/v1/action1'},
                    {name: 'tool_2', type: 'tool/v1/action2'},
                ],
            },
            clusterRoot: true,
            name: 'aiAgent_1',
            type: 'aiAgent/v1/AGENT',
        } as unknown as WorkflowTask;
        const workflow = makeWorkflow([rootTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            data: {
                clusterElementType: 'tools',
                componentName: 'tool',
                name: 'tool_1',
                parentClusterRootId: 'aiAgent_1',
                topLevelClusterRootId: 'aiAgent_1',
                workflowNodeName: 'tool_1',
            } as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const updatedTasks = getUpdatedTasks(mutation);

        expect(updatedTasks).toHaveLength(1);
        expect(updatedTasks[0].name).toBe('aiAgent_1');
        expect(updatedTasks[0].clusterElements.tools.map((tool: {name: string}) => tool.name)).toEqual(['tool_2']);
    });

    it('deletes a depth-2 nested cluster element (a tool that is itself a cluster root)', () => {
        const rootTask = {
            clusterElements: {
                tools: [
                    {
                        clusterElements: {
                            tools: [{name: 'tool_2', type: 'tool/v1/action'}],
                        },
                        name: 'agentTool_1',
                        type: 'openaiAgent/v1/AGENT',
                    },
                ],
            },
            clusterRoot: true,
            name: 'aiAgent_1',
            type: 'aiAgent/v1/AGENT',
        } as unknown as WorkflowTask;
        const workflow = makeWorkflow([rootTask]);
        const mutation = makeMockMutation();

        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            data: {
                clusterElementType: 'tools',
                componentName: 'tool',
                name: 'tool_2',
                parentClusterRootId: 'agentTool_1',
                topLevelClusterRootId: 'aiAgent_1',
                workflowNodeName: 'tool_2',
            } as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const updatedTasks = getUpdatedTasks(mutation);

        expect(updatedTasks).toHaveLength(1);

        const updatedRootTask = updatedTasks[0];

        expect(updatedRootTask.name).toBe('aiAgent_1');
        expect(updatedRootTask.clusterElements.tools).toHaveLength(1);
        expect(updatedRootTask.clusterElements.tools[0].name).toBe('agentTool_1');
        expect(updatedRootTask.clusterElements.tools[0].clusterElements.tools).toEqual([]);
    });

    it('deletes an ordinary (non-cluster) task via the plain top-level path', () => {
        const tasks = [
            {name: 'task_1', type: 'test/task_1'} as WorkflowTask,
            {name: 'task_2', type: 'test/task_2'} as WorkflowTask,
        ];
        const workflow = makeWorkflow(tasks);
        const mutation = makeMockMutation();

        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            data: {componentName: 'test', name: 'task_1', workflowNodeName: 'task_1'} as NodeDataType,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            updateWorkflowMutation: mutation,
            workflow,
        });

        expect(mutation.mutate).toHaveBeenCalledOnce();

        const updatedTasks = getUpdatedTasks(mutation);

        expect(updatedTasks.map((task: WorkflowTask) => task.name)).toEqual(['task_2']);
    });
});

describe('handleDeleteTask cluster-element panel state', () => {
    const AI_AGENT_1_TASK = {
        clusterElements: {
            tools: [
                {name: 'tool_1', type: 'tool/v1/action1'},
                {name: 'tool_2', type: 'tool/v1/action2'},
            ],
        },
        clusterRoot: true,
        name: 'aiAgent_1',
        type: 'aiAgent/v1/AGENT',
    } as unknown as WorkflowTask;

    const TOOL_1_DATA = {
        clusterElementType: 'tools',
        componentName: 'tool',
        name: 'tool_1',
        parentClusterRootId: 'aiAgent_1',
        topLevelClusterRootId: 'aiAgent_1',
        workflowNodeName: 'tool_1',
    } as NodeDataType;

    beforeEach(() => {
        vi.clearAllMocks();
        mockWorkflowState = {};
    });

    afterEach(() => {
        clearAllWorkflowMutations();
    });

    it('leaves the stored root of another agent untouched', () => {
        const setCurrentNode = vi.fn();
        const setRootClusterElementNodeData = vi.fn();

        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            currentNode: {clusterRoot: true, name: 'aiAgent_2', workflowNodeName: 'aiAgent_2'} as NodeDataType,
            data: TOOL_1_DATA,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            rootClusterElementNodeData: {
                clusterElements: {tools: []},
                clusterRoot: true,
                name: 'aiAgent_2',
                workflowNodeName: 'aiAgent_2',
            } as unknown as NodeDataType,
            setCurrentNode,
            setRootClusterElementNodeData,
            updateWorkflowMutation: makeMockMutation(),
            workflow: makeWorkflow([AI_AGENT_1_TASK]),
        });

        expect(setRootClusterElementNodeData).not.toHaveBeenCalled();
        expect(setCurrentNode).not.toHaveBeenCalled();
    });

    it('closes the details panel when the open inline element is deleted', () => {
        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            currentNode: TOOL_1_DATA,
            data: TOOL_1_DATA,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            setCurrentNode: vi.fn(),
            setRootClusterElementNodeData: vi.fn(),
            updateWorkflowMutation: makeMockMutation(),
            workflow: makeWorkflow([AI_AGENT_1_TASK]),
        });

        expect(resetDetailsPanelMock).toHaveBeenCalledOnce();
    });

    it('moves the panel to the stored root when its open element is deleted in the dialog', () => {
        const setCurrentNode = vi.fn();
        const setRootClusterElementNodeData = vi.fn();
        const rootClusterElementNodeData = {
            clusterElements: AI_AGENT_1_TASK.clusterElements,
            clusterRoot: true,
            name: 'aiAgent_1',
            workflowNodeName: 'aiAgent_1',
        } as unknown as NodeDataType;

        handleDeleteTask({
            cancelWorkflowQueries: vi.fn(),
            currentNode: TOOL_1_DATA,
            data: TOOL_1_DATA,
            invalidateWorkflowQueries: vi.fn(),
            queryClient: makeQueryClient(),
            rootClusterElementNodeData,
            setCurrentNode,
            setRootClusterElementNodeData,
            updateWorkflowMutation: makeMockMutation(),
            workflow: makeWorkflow([AI_AGENT_1_TASK]),
        });

        expect(setRootClusterElementNodeData).toHaveBeenCalledWith(expect.objectContaining({name: 'aiAgent_1'}));
        expect(setCurrentNode).toHaveBeenLastCalledWith(expect.objectContaining({name: 'aiAgent_1'}));
        expect(resetDetailsPanelMock).not.toHaveBeenCalled();
    });
});
