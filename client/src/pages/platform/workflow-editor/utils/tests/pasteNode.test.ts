import {ON_ERROR_WIRE_KEY_ERROR_BRANCH, ON_ERROR_WIRE_KEY_MAIN_BRANCH, SPACE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import pasteNode from '../pasteNode';

const hoisted = vi.hoisted(() => ({
    dataState: {
        nodes: [] as Array<Node>,
        workflow: {definition: undefined as string | undefined, id: 'workflow-1' as string | undefined},
    },
    editorState: {
        copiedNode: undefined as Record<string, unknown> | undefined,
        copiedWorkflowId: 'workflow-1' as string | undefined,
        setResetWorkflowLayout: vi.fn(),
    },
    saveWorkflowDefinition: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: {getState: () => hoisted.dataState},
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: {getState: () => hoisted.editorState},
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveWorkflowDefinition', () => ({
    default: hoisted.saveWorkflowDefinition,
}));

type SavedParametersType = NonNullable<NodeDataType['parameters']>;

const updateWorkflowMutation = {mutate: vi.fn()} as unknown as UpdateWorkflowMutationType;

function makeTask(name: string, type: string, overrides: Partial<WorkflowTask> = {}): WorkflowTask {
    return {name, type, ...overrides};
}

function makeNode(id: string, data: Partial<NodeDataType> = {}): Node {
    return {
        data: {componentName: id.split('_')[0], label: id, name: id, ...data},
        id,
        position: {x: 0, y: 0},
    };
}

function setWorkflow(tasks: Array<WorkflowTask>, nodes: Array<Node> = tasks.map((task) => makeNode(task.name))) {
    hoisted.dataState.nodes = nodes;
    hoisted.dataState.workflow = {definition: JSON.stringify({tasks}, null, SPACE), id: 'workflow-1'};
}

function setCopiedNode(copiedNode: Partial<NodeDataType>) {
    hoisted.editorState.copiedNode = {componentName: 'slack', name: 'slack_1', ...copiedNode};
}

function getSavedArguments() {
    expect(hoisted.saveWorkflowDefinition).toHaveBeenCalledTimes(1);

    return hoisted.saveWorkflowDefinition.mock.calls[0][0];
}

function getSavedNodeData(): NodeDataType {
    return getSavedArguments().nodeData;
}

describe('pasteNode', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.editorState.copiedNode = undefined;
        hoisted.editorState.copiedWorkflowId = 'workflow-1';

        setWorkflow([makeTask('slack_1', 'slack/v1/send')]);
    });

    describe('guards', () => {
        it('does nothing when no node was copied', () => {
            pasteNode({updateWorkflowMutation});

            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
        });

        it('does nothing when the node was copied from another workflow', () => {
            setCopiedNode({label: 'Slack'});

            hoisted.editorState.copiedWorkflowId = 'workflow-2';

            pasteNode({updateWorkflowMutation});

            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
        });

        it('does nothing when the workflow has no definition', () => {
            setCopiedNode({label: 'Slack'});

            hoisted.dataState.workflow = {definition: undefined, id: 'workflow-1'};

            pasteNode({updateWorkflowMutation});

            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
        });

        it('does nothing when the workflow definition is not valid JSON', () => {
            setCopiedNode({label: 'Slack'});

            hoisted.dataState.workflow = {definition: '{not json', id: 'workflow-1'};

            pasteNode({updateWorkflowMutation});

            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
        });
    });

    describe('saved node data', () => {
        it('saves the copy under a new unique name and passes the mutation and context through', () => {
            const cancelWorkflowQueries = vi.fn();
            const taskDispatcherContext = {loopId: 'loop_1', taskDispatcherId: 'loop_1'};

            setCopiedNode({label: 'Send message', operationName: 'send', version: 1});

            pasteNode({cancelWorkflowQueries, taskDispatcherContext, updateWorkflowMutation});

            const savedArguments = getSavedArguments();

            expect(cancelWorkflowQueries).toHaveBeenCalledTimes(1);
            expect(savedArguments.nodeIndex).toBeUndefined();
            expect(savedArguments.taskDispatcherContext).toBe(taskDispatcherContext);
            expect(savedArguments.updateWorkflowMutation).toBe(updateWorkflowMutation);
            expect(savedArguments.nodeData).toMatchObject({
                componentName: 'slack',
                label: 'Send message',
                name: 'slack_2',
                operationName: 'send',
                version: 1,
                workflowNodeName: 'Send message',
            });
        });

        it('strips null, undefined, empty-string, empty-array and empty-object parameters recursively', () => {
            setCopiedNode({
                parameters: {
                    blank: '',
                    channel: 'general',
                    count: 0,
                    emptyList: [],
                    enabled: false,
                    missing: undefined,
                    nested: {empty: null, inner: {deeper: ''}, kept: 'value'},
                    nothing: null,
                    onlyEmpty: {value: ''},
                    recipients: ['', null],
                },
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().parameters).toEqual({
                channel: 'general',
                count: 0,
                enabled: false,
                nested: {kept: 'value'},
                recipients: ['', null],
            });
        });

        it('does not mutate the copied node parameters', () => {
            const parameters = {blank: '', caseTrue: [makeTask('slack_1', 'slack/v1/send')]};

            setCopiedNode({componentName: 'condition', name: 'condition_1', parameters});

            pasteNode({updateWorkflowMutation});

            expect(parameters).toEqual({blank: '', caseTrue: [makeTask('slack_1', 'slack/v1/send')]});
        });

        it.each([
            {expectedMetadata: undefined, metadata: {ui: {nodePosition: {x: 1, y: 2}}}},
            {expectedMetadata: undefined, metadata: {}},
            {
                expectedMetadata: {ui: {condition: 'x > 1'}},
                metadata: {ui: {condition: 'x > 1', nodePosition: {x: 1, y: 2}}},
            },
            {expectedMetadata: {tag: 'keep'}, metadata: {tag: 'keep', ui: {nodePosition: {x: 1, y: 2}}}},
        ])('cleans metadata $metadata to $expectedMetadata', ({expectedMetadata, metadata}) => {
            setCopiedNode({metadata: metadata as NodeDataType['metadata']});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().metadata).toEqual(expectedMetadata);
        });
    });

    describe('labels', () => {
        it('keeps a label no other node uses', () => {
            setCopiedNode({label: 'Unique label'});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().label).toBe('Unique label');
        });

        it('appends the next free counter to a label that is already used', () => {
            setWorkflow(
                [makeTask('slack_1', 'slack/v1/send')],
                [
                    makeNode('slack_1', {label: 'Slack'}),
                    makeNode('slack_2', {label: 'Slack (2)'}),
                    makeNode('slack_3', {label: 'Slack (other)'}),
                ]
            );

            setCopiedNode({label: 'Slack'});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData()).toMatchObject({label: 'Slack (3)', workflowNodeName: 'Slack (3)'});
        });

        it('starts the counter at one and escapes regular expression characters in the label', () => {
            setWorkflow(
                [makeTask('slack_1', 'slack/v1/send')],
                [makeNode('slack_1', {label: 'Send (v1)?'}), makeNode('slack_2', {label: 'Send v1 (4)'})]
            );

            setCopiedNode({label: 'Send (v1)?'});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().label).toBe('Send (v1)? (1)');
        });

        it('uses an empty label when the copied node has none', () => {
            setCopiedNode({label: undefined});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().label).toBe('');
        });
    });

    describe('insert index', () => {
        beforeEach(() => {
            setWorkflow(
                [
                    makeTask('slack_1', 'slack/v1/send'),
                    makeTask('loop_1', 'loop/v1'),
                    makeTask('gmail_1', 'gmail/v1/send'),
                ],
                [
                    makeNode('manual_1', {trigger: true}),
                    makeNode('slack_1'),
                    makeNode('loop_1'),
                    makeNode('loop_1-loop-bottom-ghost', {taskDispatcherId: 'loop_1'}),
                    makeNode('gmail_1'),
                ]
            );

            setCopiedNode({label: 'Slack'});
        });

        it('uses an explicit node index over the source node', () => {
            pasteNode({nodeIndex: 7, sourceNodeName: 'slack_1', updateWorkflowMutation});

            expect(getSavedArguments().nodeIndex).toBe(7);
        });

        it('inserts after a top-level source task', () => {
            pasteNode({sourceNodeName: 'loop_1', updateWorkflowMutation});

            expect(getSavedArguments().nodeIndex).toBe(2);
        });

        it('inserts after the dispatcher that owns a non-task source node', () => {
            pasteNode({sourceNodeName: 'loop_1-loop-bottom-ghost', updateWorkflowMutation});

            expect(getSavedArguments().nodeIndex).toBe(2);
        });

        it('inserts at the start when the source node is a trigger', () => {
            pasteNode({sourceNodeName: 'manual_1', updateWorkflowMutation});

            expect(getSavedArguments().nodeIndex).toBe(0);
        });

        it('leaves the index undefined for an unknown source node', () => {
            pasteNode({sourceNodeName: 'unknown_1', updateWorkflowMutation});

            expect(getSavedArguments().nodeIndex).toBeUndefined();
        });

        it('leaves the index to the dispatcher context when pasting into a dispatcher', () => {
            pasteNode({
                sourceNodeName: 'slack_1',
                taskDispatcherContext: {loopId: 'loop_1', taskDispatcherId: 'loop_1'},
                updateWorkflowMutation,
            });

            expect(getSavedArguments().nodeIndex).toBeUndefined();
        });
    });

    describe('layout reset', () => {
        it('requests a layout reset when any node has a stored position', () => {
            setWorkflow(
                [makeTask('slack_1', 'slack/v1/send')],
                [makeNode('slack_1', {metadata: {ui: {nodePosition: {x: 10, y: 20}}}})]
            );

            setCopiedNode({label: 'Slack'});

            pasteNode({updateWorkflowMutation});

            expect(hoisted.editorState.setResetWorkflowLayout).toHaveBeenCalledWith(true);
        });

        it('does not request a layout reset when no node has a stored position', () => {
            setCopiedNode({label: 'Slack'});

            pasteNode({updateWorkflowMutation});

            expect(hoisted.editorState.setResetWorkflowLayout).not.toHaveBeenCalled();
        });
    });

    describe('task dispatcher subtasks', () => {
        it('renames condition subtasks uniquely and clears their stored positions', () => {
            const caseTrueTask = makeTask('slack_1', 'slack/v1/send', {
                metadata: {ui: {condition: 'keep', nodePosition: {x: 5, y: 5}}},
            });

            setWorkflow(
                [makeTask('condition_1', 'condition/v1', {parameters: {caseFalse: [], caseTrue: [caseTrueTask]}})],
                [makeNode('condition_1'), makeNode('slack_1')]
            );

            setCopiedNode({
                componentName: 'condition',
                name: 'condition_1',
                parameters: {
                    caseFalse: [makeTask('slack_1', 'slack/v1/send')],
                    caseTrue: [caseTrueTask, makeTask('gmail_1', 'gmail/v1/send')],
                },
            });

            pasteNode({updateWorkflowMutation});

            const savedNodeData = getSavedNodeData();

            expect(savedNodeData.name).toBe('condition_2');
            expect(savedNodeData.parameters?.caseTrue.map((task: WorkflowTask) => task.name)).toEqual([
                'slack_2',
                'gmail_1',
            ]);
            expect(savedNodeData.parameters?.caseFalse.map((task: WorkflowTask) => task.name)).toEqual(['slack_3']);
            expect(savedNodeData.parameters?.caseTrue[0].metadata.ui).toEqual({
                condition: 'keep',
                nodePosition: undefined,
            });
        });

        it('drops empty subtask collections from the pasted dispatcher', () => {
            setCopiedNode({
                componentName: 'condition',
                name: 'condition_1',
                parameters: {caseFalse: [], caseTrue: [makeTask('slack_1', 'slack/v1/send')]},
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().parameters).not.toHaveProperty('caseFalse');
        });

        it('renames tasks in nested dispatchers recursively', () => {
            setCopiedNode({
                componentName: 'loop',
                name: 'loop_1',
                parameters: {
                    iteratee: [
                        makeTask('loop_2', 'loop/v1', {
                            parameters: {iteratee: [makeTask('slack_1', 'slack/v1/send')]},
                        }),
                    ],
                },
            });

            pasteNode({updateWorkflowMutation});

            const savedNodeData = getSavedNodeData();

            expect(savedNodeData.name).toBe('loop_1');
            expect(savedNodeData.parameters?.iteratee[0].name).toBe('loop_2');
            expect(savedNodeData.parameters?.iteratee[0].parameters.iteratee[0].name).toBe('slack_2');
        });

        it.each([
            {
                componentName: 'branch',
                getSubtaskNames: (parameters: SavedParametersType) => [
                    ...parameters.cases.flatMap((caseItem: {tasks: WorkflowTask[]}) =>
                        caseItem.tasks.map((task) => task.name)
                    ),
                    ...parameters.default.map((task: WorkflowTask) => task.name),
                ],
                parameters: {
                    cases: [
                        {key: 'case_0', tasks: [makeTask('slack_1', 'slack/v1/send')]},
                        {key: 'case_1', tasks: [makeTask('slack_1', 'slack/v1/send')]},
                    ],
                    default: [makeTask('slack_1', 'slack/v1/send')],
                },
            },
            {
                componentName: 'map',
                getSubtaskNames: (parameters: SavedParametersType) =>
                    parameters.iteratee.map((task: WorkflowTask) => task.name),
                parameters: {
                    iteratee: [
                        makeTask('slack_1', 'slack/v1/send'),
                        makeTask('slack_1', 'slack/v1/send'),
                        makeTask('slack_1', 'slack/v1/send'),
                    ],
                },
            },
            {
                componentName: 'parallel',
                getSubtaskNames: (parameters: SavedParametersType) =>
                    parameters.tasks.map((task: WorkflowTask) => task.name),
                parameters: {
                    tasks: [
                        makeTask('slack_1', 'slack/v1/send'),
                        makeTask('slack_1', 'slack/v1/send'),
                        makeTask('slack_1', 'slack/v1/send'),
                    ],
                },
            },
            {
                componentName: 'fork-join',
                getSubtaskNames: (parameters: SavedParametersType) =>
                    parameters.branches.flat().map((task: WorkflowTask) => task.name),
                parameters: {
                    branches: [
                        [makeTask('slack_1', 'slack/v1/send'), makeTask('slack_1', 'slack/v1/send')],
                        [makeTask('slack_1', 'slack/v1/send')],
                    ],
                },
            },
            {
                componentName: 'on-error',
                getSubtaskNames: (parameters: SavedParametersType) => [
                    ...parameters[ON_ERROR_WIRE_KEY_MAIN_BRANCH].map((task: WorkflowTask) => task.name),
                    ...parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH].map((task: WorkflowTask) => task.name),
                ],
                parameters: {
                    [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [makeTask('slack_1', 'slack/v1/send')],
                    [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [
                        makeTask('slack_1', 'slack/v1/send'),
                        makeTask('slack_1', 'slack/v1/send'),
                    ],
                },
            },
        ])('renames every $componentName subtask uniquely', ({componentName, getSubtaskNames, parameters}) => {
            setCopiedNode({componentName, name: `${componentName}_1`, parameters});

            pasteNode({updateWorkflowMutation});

            expect(getSubtaskNames(getSavedNodeData().parameters ?? {})).toEqual(['slack_2', 'slack_3', 'slack_4']);
        });

        it('renames the single each iteratee', () => {
            setCopiedNode({
                componentName: 'each',
                name: 'each_1',
                parameters: {items: 'itemsExpression', iteratee: makeTask('slack_1', 'slack/v1/send')},
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().parameters).toEqual({
                items: 'itemsExpression',
                iteratee: {name: 'slack_2', type: 'slack/v1/send'},
            });
        });

        it('leaves subtasks without a type untouched', () => {
            setCopiedNode({
                componentName: 'parallel',
                name: 'parallel_1',
                parameters: {tasks: [{name: 'untyped_1'}]},
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().parameters?.tasks).toEqual([{name: 'untyped_1'}]);
        });

        it('does not rename nested collections of a non-dispatcher component', () => {
            setCopiedNode({parameters: {tasks: [makeTask('slack_1', 'slack/v1/send')]}});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().parameters?.tasks[0].name).toBe('slack_1');
        });
    });

    describe('cluster elements', () => {
        const definitionClusterElements = {
            memory: null,
            model: {
                metadata: {ui: {nodePosition: {x: 1, y: 1}, placeholderPositions: {left: {x: 0, y: 0}}}},
                name: 'openAi_1',
                type: 'openAi/v1/model',
            },
            tools: [
                {
                    clusterElements: {
                        model: {name: 'openAi_2', type: 'openAi/v1/model'},
                    },
                    name: 'aiAgent_1',
                    type: 'aiAgent/v1/tool',
                },
                {name: 'slack_9', type: 'slack/v1/send'},
            ],
        };

        it('copies the cluster elements from the workflow definition and renames them uniquely', () => {
            setWorkflow(
                [makeTask('aiAgent_1', 'aiAgent/v1/chat', {clusterElements: definitionClusterElements})],
                [makeNode('aiAgent_1')]
            );

            setCopiedNode({
                clusterElements: {model: {name: 'stale_1', type: 'stale/v1/model'}},
                componentName: 'aiAgent',
                name: 'aiAgent_1',
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData()).toMatchObject({
                clusterElements: {
                    memory: null,
                    model: {
                        metadata: {ui: {nodePosition: undefined, placeholderPositions: {left: {x: 0, y: 0}}}},
                        name: 'openAi_3',
                        type: 'openAi/v1/model',
                    },
                    tools: [
                        {
                            clusterElements: {model: {name: 'openAi_4', type: 'openAi/v1/model'}},
                            name: 'aiAgent_3',
                            type: 'aiAgent/v1/tool',
                        },
                        {name: 'slack_10', type: 'slack/v1/send'},
                    ],
                },
                name: 'aiAgent_2',
            });
        });

        it('looks the definition task up by the copied workflow node name', () => {
            setWorkflow(
                [makeTask('aiAgent_1', 'aiAgent/v1/chat', {clusterElements: definitionClusterElements})],
                [makeNode('aiAgent_1')]
            );

            setCopiedNode({componentName: 'aiAgent', name: 'other', workflowNodeName: 'aiAgent_1'});

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().clusterElements).toHaveProperty('tools');
        });

        it('falls back to the copied cluster elements when the task is not in the definition', () => {
            setCopiedNode({
                clusterElements: {model: {name: 'openAi_1', type: 'openAi/v1/model'}},
                componentName: 'aiAgent',
                name: 'aiAgent_7',
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().clusterElements).toEqual({
                model: {metadata: undefined, name: 'openAi_1', type: 'openAi/v1/model'},
            });
        });

        it('passes array cluster element definitions through unchanged', () => {
            const clusterElementDefinitions = [{name: 'model', type: 'openAi/v1/model'}];

            setCopiedNode({
                clusterElements: clusterElementDefinitions as NodeDataType['clusterElements'],
                componentName: 'aiAgent',
                name: 'aiAgent_7',
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().clusterElements).toBe(clusterElementDefinitions);
        });

        it('keeps a cluster element without a type as is', () => {
            setCopiedNode({
                clusterElements: {model: {name: 'custom'} as never},
                componentName: 'aiAgent',
                name: 'aiAgent_7',
            });

            pasteNode({updateWorkflowMutation});

            expect(getSavedNodeData().clusterElements).toEqual({model: {name: 'custom'}});
        });
    });
});
