import {
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    ON_ERROR_ERROR_BRANCH,
    ON_ERROR_MAIN_BRANCH,
    ON_ERROR_WIRE_KEY_ERROR_BRANCH,
    ON_ERROR_WIRE_KEY_MAIN_BRANCH,
} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType, PropertyAllType, TaskDispatcherContextType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    TASK_DISPATCHER_CONFIG,
    buildGenericNodeData,
    getParentTaskDispatcherTask,
    getTaskDispatcherTask,
} from '../taskDispatcherConfig';

function makeTask(name: string, type: string, parameters?: WorkflowTask['parameters']): WorkflowTask {
    return {name, parameters, type};
}

function makeNode(parameters: Record<string, unknown>): Node {
    return {
        data: {componentName: 'any', name: 'any', parameters},
        id: 'node-1',
        position: {x: 0, y: 0},
    };
}

const baseNodeData: NodeDataType = {
    componentName: 'slack',
    name: 'slack_1',
    workflowNodeName: 'slack_1',
};

const defaultValueProperties = [{defaultValue: 'GET', name: 'method'}] as unknown as Array<PropertyAllType>;

describe('buildGenericNodeData', () => {
    it('sets the dispatcher identifier and condition data with defaults', () => {
        const nodeData = buildGenericNodeData(
            baseNodeData,
            {conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            'condition_1',
            'condition'
        );

        expect(nodeData).toEqual({
            ...baseNodeData,
            conditionData: {conditionCase: CONDITION_CASE_TRUE, conditionId: 'condition_1', index: 0},
            conditionId: 'condition_1',
            taskDispatcherId: 'condition_1',
        });
    });

    it('keeps the context condition case and index', () => {
        const nodeData = buildGenericNodeData(
            baseNodeData,
            {
                conditionCase: CONDITION_CASE_FALSE,
                conditionId: 'condition_1',
                index: 3,
                taskDispatcherId: 'condition_1',
            },
            'condition_1',
            'condition'
        );

        expect(nodeData.conditionData).toEqual({
            conditionCase: CONDITION_CASE_FALSE,
            conditionId: 'condition_1',
            index: 3,
        });
    });

    it.each([
        {
            context: {index: 2, loopId: 'loop_1'},
            dataKey: 'loopData',
            dispatcherId: 'loop_1',
            dispatcherType: 'loop',
            expectedData: {index: 2, loopId: 'loop_1'},
        },
        {
            context: {loopBreakId: 'loopBreak_1'},
            dataKey: 'loopBreakData',
            dispatcherId: 'loopBreak_1',
            dispatcherType: 'loopBreak',
            expectedData: {loopBreakId: 'loopBreak_1'},
        },
        {
            context: {mapId: 'map_1'},
            dataKey: 'mapData',
            dispatcherId: 'map_1',
            dispatcherType: 'map',
            expectedData: {index: 0, mapId: 'map_1'},
        },
        {
            context: {subflowId: 'subflow_1'},
            dataKey: 'subflowData',
            dispatcherId: 'subflow_1',
            dispatcherType: 'subflow',
            expectedData: {subflowId: 'subflow_1'},
        },
        {
            context: {terminateId: 'terminate_1'},
            dataKey: 'terminateData',
            dispatcherId: 'terminate_1',
            dispatcherType: 'terminate',
            expectedData: {terminateId: 'terminate_1'},
        },
        {
            context: {branchId: 'branch_1'},
            dataKey: 'branchData',
            dispatcherId: 'branch_1',
            dispatcherType: 'branch',
            expectedData: {branchId: 'branch_1', caseKey: 'default', index: 0},
        },
        {
            context: {branchId: 'branch_1', caseKey: 'case_1', index: 4},
            dataKey: 'branchData',
            dispatcherId: 'branch_1',
            dispatcherType: 'branch',
            expectedData: {branchId: 'branch_1', caseKey: 'case_1', index: 4},
        },
        {
            context: {index: 1, parallelId: 'parallel_1'},
            dataKey: 'parallelData',
            dispatcherId: 'parallel_1',
            dispatcherType: 'parallel',
            expectedData: {index: 1, parallelId: 'parallel_1'},
        },
        {
            context: {eachId: 'each_1', index: 5},
            dataKey: 'eachData',
            dispatcherId: 'each_1',
            dispatcherType: 'each',
            expectedData: {eachId: 'each_1', index: 0},
        },
        {
            context: {forkJoinId: 'forkJoin_1'},
            dataKey: 'forkJoinData',
            dispatcherId: 'forkJoin_1',
            dispatcherType: 'fork-join',
            expectedData: {branchIndex: 0, forkJoinId: 'forkJoin_1', index: 0},
        },
        {
            context: {branchIndex: 2, forkJoinId: 'forkJoin_1', index: 1},
            dataKey: 'forkJoinData',
            dispatcherId: 'forkJoin_1',
            dispatcherType: 'fork-join',
            expectedData: {branchIndex: 2, forkJoinId: 'forkJoin_1', index: 1},
        },
        {
            context: {onErrorId: 'onError_1'},
            dataKey: 'onErrorData',
            dispatcherId: 'onError_1',
            dispatcherType: 'on-error',
            expectedData: {index: 0, onErrorCase: ON_ERROR_MAIN_BRANCH, onErrorId: 'onError_1'},
        },
        {
            context: {index: 2, onErrorCase: ON_ERROR_ERROR_BRANCH, onErrorId: 'onError_1'},
            dataKey: 'onErrorData',
            dispatcherId: 'onError_1',
            dispatcherType: 'on-error',
            expectedData: {index: 2, onErrorCase: ON_ERROR_ERROR_BRANCH, onErrorId: 'onError_1'},
        },
    ] as const)(
        'builds $dataKey for a $dispatcherType context $context',
        ({context, dataKey, dispatcherId, dispatcherType, expectedData}) => {
            const nodeData = buildGenericNodeData(
                baseNodeData,
                {...context, taskDispatcherId: dispatcherId} as TaskDispatcherContextType,
                dispatcherId,
                dispatcherType
            );

            expect(nodeData[dataKey]).toEqual(expectedData);
            expect(nodeData.taskDispatcherId).toBe(dispatcherId);
            expect(nodeData[TASK_DISPATCHER_CONFIG[dispatcherType].contextIdentifier as keyof NodeDataType]).toBe(
                dispatcherId
            );
        }
    );

    it('only sets the identifier when the context carries no dispatcher id', () => {
        const nodeData = buildGenericNodeData(baseNodeData, {taskDispatcherId: 'loop_1'}, 'loop_1', 'loop');

        expect(nodeData).toEqual({...baseNodeData, loopId: 'loop_1'});
    });

    it('uses the first dispatcher id found in config order when the context carries several', () => {
        const nodeData = buildGenericNodeData(
            baseNodeData,
            {branchId: 'branch_1', loopId: 'loop_1', taskDispatcherId: 'loop_1'},
            'loop_1',
            'loop'
        );

        expect(nodeData.loopId).toBe('loop_1');
        expect(nodeData.branchData).toEqual({branchId: 'branch_1', caseKey: 'default', index: 0});
        expect(nodeData.loopData).toBeUndefined();
        expect(nodeData.taskDispatcherId).toBe('branch_1');
    });

    it('does not mutate the base node data', () => {
        const originalNodeData = {...baseNodeData};

        buildGenericNodeData(baseNodeData, {loopId: 'loop_1', taskDispatcherId: 'loop_1'}, 'loop_1', 'loop');

        expect(baseNodeData).toEqual(originalNodeData);
    });
});

describe('TASK_DISPATCHER_CONFIG', () => {
    describe('shared entries', () => {
        it.each(Object.keys(TASK_DISPATCHER_CONFIG) as Array<keyof typeof TASK_DISPATCHER_CONFIG>)(
            '%s.buildNodeData delegates to buildGenericNodeData',
            (dispatcherType) => {
                const config = TASK_DISPATCHER_CONFIG[dispatcherType];

                const taskDispatcherContext = {
                    [config.contextIdentifier]: 'dispatcher_1',
                    taskDispatcherId: 'dispatcher_1',
                } as TaskDispatcherContextType;

                expect(
                    config.buildNodeData({baseNodeData, taskDispatcherContext, taskDispatcherId: 'dispatcher_1'})
                ).toEqual(buildGenericNodeData(baseNodeData, taskDispatcherContext, 'dispatcher_1', dispatcherType));
            }
        );

        it.each(Object.keys(TASK_DISPATCHER_CONFIG) as Array<keyof typeof TASK_DISPATCHER_CONFIG>)(
            '%s.getDispatcherId reads its own context identifier',
            (dispatcherType) => {
                const config = TASK_DISPATCHER_CONFIG[dispatcherType];

                const context = {
                    [config.contextIdentifier]: 'dispatcher_1',
                    taskDispatcherId: 'other',
                } as TaskDispatcherContextType;

                expect(config.getDispatcherId(context)).toBe('dispatcher_1');
                expect(config.getDispatcherId({taskDispatcherId: 'dispatcher_1'})).toBeUndefined();
            }
        );

        it.each(Object.keys(TASK_DISPATCHER_CONFIG) as Array<keyof typeof TASK_DISPATCHER_CONFIG>)(
            '%s.getInitialParameters includes property default values',
            (dispatcherType) => {
                const initialParameters =
                    TASK_DISPATCHER_CONFIG[dispatcherType].getInitialParameters(defaultValueProperties);

                expect(initialParameters).toMatchObject({method: 'GET'});
            }
        );

        it.each(Object.keys(TASK_DISPATCHER_CONFIG) as Array<keyof typeof TASK_DISPATCHER_CONFIG>)(
            '%s.getTask is getTaskDispatcherTask',
            (dispatcherType) => {
                expect(TASK_DISPATCHER_CONFIG[dispatcherType].getTask).toBe(getTaskDispatcherTask);
            }
        );

        it('dataKey matches the node data property each dispatcher populates', () => {
            expect(
                Object.fromEntries(
                    Object.entries(TASK_DISPATCHER_CONFIG).map(([type, config]) => [type, config.dataKey])
                )
            ).toEqual({
                branch: 'branchData',
                condition: 'conditionData',
                each: 'eachData',
                'fork-join': 'forkJoinData',
                loop: 'loopData',
                loopBreak: 'loopBreakData',
                map: 'mapData',
                'on-error': 'onErrorData',
                parallel: 'parallelData',
                subflow: 'subflowData',
                terminate: 'terminateData',
            });
        });
    });

    describe('branch', () => {
        const branchConfig = TASK_DISPATCHER_CONFIG.branch;

        const branchTask = makeTask('branch_1', 'branch/v1', {
            cases: [
                {key: 'case_0', tasks: [makeTask('slack_1', 'slack/v1/send')]},
                {key: 1, tasks: [makeTask('slack_2', 'slack/v1/send')]},
            ],
            default: [makeTask('slack_3', 'slack/v1/send')],
        });

        it('extracts a string case key and index from a placeholder id', () => {
            expect(branchConfig.extractContextFromPlaceholder('branch_1-branch-case_0-placeholder-2')).toEqual({
                caseKey: 'case_0',
                index: 2,
                taskDispatcherId: 'branch_1',
            });
        });

        it('parses numeric case keys from a placeholder id as numbers', () => {
            expect(branchConfig.extractContextFromPlaceholder('branch_1-branch-3-placeholder-0').caseKey).toBe(3);
            expect(branchConfig.extractContextFromPlaceholder('branch_1-branch-1.5-placeholder-0').caseKey).toBe(1.5);
        });

        it('returns initial parameters with a single empty case and an empty default', () => {
            expect(branchConfig.getInitialParameters(defaultValueProperties)).toEqual({
                cases: [{key: 'case_0', tasks: []}],
                default: [],
                method: 'GET',
            });
        });

        it('initializes empty cases and default', () => {
            expect(branchConfig.initializeParameters()).toEqual({cases: [], default: []});
        });

        describe('getNewCaseKey', () => {
            it('returns case_0 when there are no cases', () => {
                expect(branchConfig.getNewCaseKey(undefined as unknown as Array<BranchCaseType>)).toBe('case_0');
                expect(branchConfig.getNewCaseKey([])).toBe('case_0');
            });

            it('returns a key based on the number of cases', () => {
                expect(
                    branchConfig.getNewCaseKey([
                        {key: 'case_0', tasks: []},
                        {key: 'case_1', tasks: []},
                    ])
                ).toBe('case_2');
            });

            it('adds a suffix when the computed key already exists', () => {
                expect(branchConfig.getNewCaseKey([{key: 'case_1', tasks: []}])).toBe('case_1 (1)');
            });
        });

        describe('getSubtasks', () => {
            it('returns an empty array when there are no parameters', () => {
                expect(branchConfig.getSubtasks({task: makeTask('branch_1', 'branch/v1')})).toEqual([]);
                expect(branchConfig.getSubtasks({})).toEqual([]);
            });

            it('returns default and every case task when getAllSubtasks is set', () => {
                const subtaskNames = branchConfig
                    .getSubtasks({getAllSubtasks: true, task: branchTask})
                    .map((subtask) => subtask.name);

                expect(subtaskNames).toEqual(['slack_3', 'slack_1', 'slack_2']);
            });

            it('returns the default tasks when no case key is given', () => {
                expect(branchConfig.getSubtasks({task: branchTask}).map((subtask) => subtask.name)).toEqual([
                    'slack_3',
                ]);
            });

            it('matches case keys across string and number forms', () => {
                expect(
                    branchConfig
                        .getSubtasks({context: {caseKey: '1', taskDispatcherId: 'branch_1'}, task: branchTask})
                        .map((subtask) => subtask.name)
                ).toEqual(['slack_2']);
            });

            it('returns an empty array for an unknown case key', () => {
                expect(
                    branchConfig.getSubtasks({
                        context: {caseKey: 'missing', taskDispatcherId: 'branch_1'},
                        task: branchTask,
                    })
                ).toEqual([]);
            });

            it('prefers node parameters over task parameters', () => {
                const node = makeNode({default: [makeTask('node_default', 'slack/v1/send')]});

                expect(branchConfig.getSubtasks({node, task: branchTask}).map((subtask) => subtask.name)).toEqual([
                    'node_default',
                ]);
            });
        });

        describe('updateTaskParameters', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            it('replaces the default tasks when no case key is given', () => {
                const updatedTask = branchConfig.updateTaskParameters({task: branchTask, updatedSubtasks});

                expect(updatedTask.parameters?.default).toBe(updatedSubtasks);
                expect(updatedTask.parameters?.cases).toBe(branchTask.parameters?.cases);
            });

            it('replaces the tasks of an existing case, matching numeric keys loosely', () => {
                const updatedTask = branchConfig.updateTaskParameters({
                    context: {caseKey: '1', taskDispatcherId: 'branch_1'},
                    task: branchTask,
                    updatedSubtasks,
                });

                expect(updatedTask.parameters?.cases).toEqual([
                    branchTask.parameters?.cases[0],
                    {key: 1, tasks: updatedSubtasks},
                ]);
                expect(branchTask.parameters?.cases[1].tasks[0].name).toBe('slack_2');
            });

            it('appends a new case when the case key does not exist', () => {
                const updatedTask = branchConfig.updateTaskParameters({
                    context: {caseKey: 'case_5', taskDispatcherId: 'branch_1'},
                    task: branchTask,
                    updatedSubtasks,
                });

                expect(updatedTask.parameters?.cases).toHaveLength(3);
                expect(updatedTask.parameters?.cases[2]).toEqual({key: 'case_5', tasks: updatedSubtasks});
                expect(branchTask.parameters?.cases).toHaveLength(2);
            });

            it('creates the cases array when the task has none', () => {
                const updatedTask = branchConfig.updateTaskParameters({
                    context: {caseKey: 'case_0', taskDispatcherId: 'branch_1'},
                    task: makeTask('branch_1', 'branch/v1'),
                    updatedSubtasks,
                });

                expect(updatedTask.parameters).toEqual({cases: [{key: 'case_0', tasks: updatedSubtasks}]});
            });
        });
    });

    describe('condition', () => {
        const conditionConfig = TASK_DISPATCHER_CONFIG.condition;

        const conditionTask = makeTask('condition_1', 'condition/v1', {
            caseFalse: [makeTask('slack_2', 'slack/v1/send')],
            caseTrue: [makeTask('slack_1', 'slack/v1/send')],
        });

        it('maps left placeholders to the true case and others to the false case', () => {
            expect(conditionConfig.extractContextFromPlaceholder('condition_1-condition-left-placeholder-0')).toEqual({
                conditionCase: CONDITION_CASE_TRUE,
                index: 0,
                taskDispatcherId: 'condition_1',
            });
            expect(conditionConfig.extractContextFromPlaceholder('condition_1-condition-right-placeholder-3')).toEqual({
                conditionCase: CONDITION_CASE_FALSE,
                index: 3,
                taskDispatcherId: 'condition_1',
            });
        });

        it('returns initial and initialized parameters with both cases', () => {
            expect(conditionConfig.getInitialParameters(defaultValueProperties)).toEqual({
                caseFalse: [],
                caseTrue: [],
                method: 'GET',
            });
            expect(conditionConfig.initializeParameters()).toEqual({caseFalse: [], caseTrue: []});
        });

        it('returns subtasks for the requested case, defaulting to the true case', () => {
            expect(conditionConfig.getSubtasks({task: conditionTask}).map((subtask) => subtask.name)).toEqual([
                'slack_1',
            ]);
            expect(
                conditionConfig
                    .getSubtasks({
                        context: {conditionCase: CONDITION_CASE_FALSE, taskDispatcherId: 'condition_1'},
                        task: conditionTask,
                    })
                    .map((subtask) => subtask.name)
            ).toEqual(['slack_2']);
        });

        it('returns both cases when getAllSubtasks is set', () => {
            expect(
                conditionConfig.getSubtasks({getAllSubtasks: true, task: conditionTask}).map((subtask) => subtask.name)
            ).toEqual(['slack_1', 'slack_2']);
        });

        it('returns empty arrays for missing parameters or cases', () => {
            expect(conditionConfig.getSubtasks({task: makeTask('condition_1', 'condition/v1')})).toEqual([]);
            expect(conditionConfig.getSubtasks({task: makeTask('condition_1', 'condition/v1', {})})).toEqual([]);
            expect(
                conditionConfig.getSubtasks({
                    context: {conditionCase: CONDITION_CASE_FALSE, taskDispatcherId: 'condition_1'},
                    task: makeTask('condition_1', 'condition/v1', {}),
                })
            ).toEqual([]);
        });

        it('reads subtasks from node data when a node is given', () => {
            const node = makeNode({caseTrue: [makeTask('node_true', 'slack/v1/send')]});

            expect(conditionConfig.getSubtasks({node}).map((subtask) => subtask.name)).toEqual(['node_true']);
        });

        it('updates the requested case, defaulting to the true case', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            expect(conditionConfig.updateTaskParameters({task: conditionTask, updatedSubtasks}).parameters).toEqual({
                caseFalse: conditionTask.parameters?.caseFalse,
                caseTrue: updatedSubtasks,
            });
            expect(
                conditionConfig.updateTaskParameters({
                    context: {conditionCase: CONDITION_CASE_FALSE, taskDispatcherId: 'condition_1'},
                    task: conditionTask,
                    updatedSubtasks,
                }).parameters
            ).toEqual({caseFalse: updatedSubtasks, caseTrue: conditionTask.parameters?.caseTrue});
        });
    });

    describe('each', () => {
        const eachConfig = TASK_DISPATCHER_CONFIG.each;

        it('extracts the dispatcher id and index from a placeholder id', () => {
            expect(eachConfig.extractContextFromPlaceholder('each_1-each-placeholder-0')).toEqual({
                index: 0,
                taskDispatcherId: 'each_1',
            });
        });

        it('initializes a null iteratee', () => {
            expect(eachConfig.initializeParameters()).toEqual({iteratee: null});
            expect(eachConfig.getInitialParameters(defaultValueProperties)).toEqual({method: 'GET'});
        });

        it('wraps the single iteratee task in an array', () => {
            const iteratee = makeTask('slack_1', 'slack/v1/send');

            expect(eachConfig.getSubtasks({task: makeTask('each_1', 'each/v1', {iteratee})})).toEqual([iteratee]);
            expect(eachConfig.getSubtasks({node: makeNode({iteratee})})).toEqual([iteratee]);
        });

        it('returns an empty array when there is no iteratee', () => {
            expect(eachConfig.getSubtasks({task: makeTask('each_1', 'each/v1')})).toEqual([]);
            expect(eachConfig.getSubtasks({task: makeTask('each_1', 'each/v1', {iteratee: null})})).toEqual([]);
        });

        it('stores the first updated subtask as the iteratee, or null when there are none', () => {
            const eachTask = makeTask('each_1', 'each/v1', {items: 'itemsExpression'});
            const firstTask = makeTask('slack_1', 'slack/v1/send');

            expect(
                eachConfig.updateTaskParameters({
                    task: eachTask,
                    updatedSubtasks: [firstTask, makeTask('slack_2', 'slack/v1/send')],
                }).parameters
            ).toEqual({items: 'itemsExpression', iteratee: firstTask});
            expect(eachConfig.updateTaskParameters({task: eachTask, updatedSubtasks: []}).parameters).toEqual({
                items: 'itemsExpression',
                iteratee: null,
            });
        });
    });

    describe('fork-join', () => {
        const forkJoinConfig = TASK_DISPATCHER_CONFIG['fork-join'];

        const forkJoinTask = makeTask('forkJoin_1', 'fork-join/v1', {
            branches: [[makeTask('slack_1', 'slack/v1/send')], [makeTask('slack_2', 'slack/v1/send')]],
        });

        it('uses the placeholder index as both index and branch index', () => {
            expect(forkJoinConfig.extractContextFromPlaceholder('forkJoin_1-forkJoin-placeholder-1')).toEqual({
                branchIndex: 1,
                index: 1,
                taskDispatcherId: 'forkJoin_1',
            });
        });

        it('initializes empty branches', () => {
            expect(forkJoinConfig.initializeParameters()).toEqual({branches: []});
            expect(forkJoinConfig.getInitialParameters(defaultValueProperties)).toEqual({branches: [], method: 'GET'});
        });

        it('returns the tasks of the requested branch, defaulting to the first', () => {
            expect(forkJoinConfig.getSubtasks({task: forkJoinTask}).map((subtask) => subtask.name)).toEqual([
                'slack_1',
            ]);
            expect(
                forkJoinConfig
                    .getSubtasks({context: {branchIndex: 1, taskDispatcherId: 'forkJoin_1'}, task: forkJoinTask})
                    .map((subtask) => subtask.name)
            ).toEqual(['slack_2']);
        });

        it('returns an empty array for an out-of-range branch index', () => {
            expect(
                forkJoinConfig.getSubtasks({
                    context: {branchIndex: 5, taskDispatcherId: 'forkJoin_1'},
                    task: forkJoinTask,
                })
            ).toEqual([]);
            expect(
                forkJoinConfig.getSubtasks({
                    context: {branchIndex: -1, taskDispatcherId: 'forkJoin_1'},
                    task: forkJoinTask,
                })
            ).toEqual([]);
            expect(forkJoinConfig.getSubtasks({task: makeTask('forkJoin_1', 'fork-join/v1')})).toEqual([]);
        });

        it('flattens every branch when getAllSubtasks is set', () => {
            expect(
                forkJoinConfig.getSubtasks({getAllSubtasks: true, task: forkJoinTask}).map((subtask) => subtask.name)
            ).toEqual(['slack_1', 'slack_2']);
        });

        it('replaces the tasks of an existing branch without mutating the task', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            const updatedTask = forkJoinConfig.updateTaskParameters({
                context: {branchIndex: 1, taskDispatcherId: 'forkJoin_1'},
                task: forkJoinTask,
                updatedSubtasks,
            });

            expect(updatedTask.parameters?.branches).toEqual([forkJoinTask.parameters?.branches[0], updatedSubtasks]);
            expect(forkJoinTask.parameters?.branches[1][0].name).toBe('slack_2');
        });

        it('pads with empty branches when updating beyond the current branch count', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            const updatedTask = forkJoinConfig.updateTaskParameters({
                context: {branchIndex: 2, taskDispatcherId: 'forkJoin_1'},
                task: makeTask('forkJoin_1', 'fork-join/v1'),
                updatedSubtasks,
            });

            expect(updatedTask.parameters?.branches).toEqual([[], [], updatedSubtasks]);
        });
    });

    describe.each(['loop', 'map'] as const)('%s', (dispatcherType) => {
        const config = TASK_DISPATCHER_CONFIG[dispatcherType];

        it('extracts the dispatcher id and index from a placeholder id', () => {
            expect(config.extractContextFromPlaceholder(`${dispatcherType}_1-${dispatcherType}-placeholder-4`)).toEqual(
                {index: 4, taskDispatcherId: `${dispatcherType}_1`}
            );
        });

        it('initializes an empty iteratee array', () => {
            expect(config.initializeParameters()).toEqual({iteratee: []});
            expect(config.getInitialParameters(defaultValueProperties)).toEqual({method: 'GET'});
        });

        it('returns the iteratee tasks from the task or the node', () => {
            const iteratee = [makeTask('slack_1', 'slack/v1/send')];

            expect(
                config.getSubtasks({task: makeTask(`${dispatcherType}_1`, `${dispatcherType}/v1`, {iteratee})})
            ).toBe(iteratee);
            expect(config.getSubtasks({node: makeNode({iteratee})})).toBe(iteratee);
        });

        it('returns an empty array without parameters or iteratee', () => {
            expect(config.getSubtasks({task: makeTask(`${dispatcherType}_1`, `${dispatcherType}/v1`)})).toEqual([]);
            expect(config.getSubtasks({task: makeTask(`${dispatcherType}_1`, `${dispatcherType}/v1`, {})})).toEqual([]);
        });

        it('replaces the iteratee with the updated subtasks', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            expect(
                config.updateTaskParameters({
                    task: makeTask(`${dispatcherType}_1`, `${dispatcherType}/v1`, {items: [1], iteratee: []}),
                    updatedSubtasks,
                }).parameters
            ).toEqual({items: [1], iteratee: updatedSubtasks});
        });
    });

    describe('on-error', () => {
        const onErrorConfig = TASK_DISPATCHER_CONFIG['on-error'];

        const onErrorTask = makeTask('onError_1', 'on-error/v1', {
            [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [makeTask('slack_2', 'slack/v1/send')],
            [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [makeTask('slack_1', 'slack/v1/send')],
        });

        it('extracts the main branch from a left placeholder', () => {
            expect(onErrorConfig.extractContextFromPlaceholder('onError_1-onError-left-placeholder-2')).toEqual({
                index: 2,
                onErrorCase: ON_ERROR_MAIN_BRANCH,
                onErrorId: 'onError_1',
                taskDispatcherId: 'onError_1',
            });
        });

        it('extracts the error branch from a right placeholder', () => {
            expect(onErrorConfig.extractContextFromPlaceholder('onError_1-onError-right-placeholder-0')).toEqual({
                index: 0,
                onErrorCase: ON_ERROR_ERROR_BRANCH,
                onErrorId: 'onError_1',
                taskDispatcherId: 'onError_1',
            });
        });

        it('returns an empty context when the placeholder has no on-error marker', () => {
            expect(onErrorConfig.extractContextFromPlaceholder('loop_1-loop-placeholder-0')).toEqual({
                index: 0,
                taskDispatcherId: '',
            });
        });

        it('initializes both wire-key branches', () => {
            const expectedParameters = {
                [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [],
                [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [],
            };

            expect(onErrorConfig.initializeParameters()).toEqual(expectedParameters);
            expect(onErrorConfig.getInitialParameters(defaultValueProperties)).toEqual({
                ...expectedParameters,
                method: 'GET',
            });
        });

        it('returns the requested branch, defaulting to the main branch', () => {
            expect(onErrorConfig.getSubtasks({task: onErrorTask}).map((subtask) => subtask.name)).toEqual(['slack_1']);
            expect(
                onErrorConfig
                    .getSubtasks({
                        context: {onErrorCase: ON_ERROR_ERROR_BRANCH, taskDispatcherId: 'onError_1'},
                        task: onErrorTask,
                    })
                    .map((subtask) => subtask.name)
            ).toEqual(['slack_2']);
        });

        it('returns both branches when getAllSubtasks is set', () => {
            expect(
                onErrorConfig.getSubtasks({getAllSubtasks: true, task: onErrorTask}).map((subtask) => subtask.name)
            ).toEqual(['slack_1', 'slack_2']);
        });

        it('returns empty arrays when parameters or branches are missing', () => {
            expect(onErrorConfig.getSubtasks({task: makeTask('onError_1', 'on-error/v1')})).toEqual([]);
            expect(
                onErrorConfig.getSubtasks({getAllSubtasks: true, task: makeTask('onError_1', 'on-error/v1', {})})
            ).toEqual([]);
            expect(
                onErrorConfig.getSubtasks({
                    context: {onErrorCase: ON_ERROR_ERROR_BRANCH, taskDispatcherId: 'onError_1'},
                    task: makeTask('onError_1', 'on-error/v1', {}),
                })
            ).toEqual([]);
        });

        it('writes updated subtasks under the wire key of the requested branch', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            expect(onErrorConfig.updateTaskParameters({task: onErrorTask, updatedSubtasks}).parameters).toEqual({
                [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: onErrorTask.parameters?.[ON_ERROR_WIRE_KEY_ERROR_BRANCH],
                [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: updatedSubtasks,
            });
            expect(
                onErrorConfig.updateTaskParameters({
                    context: {onErrorCase: ON_ERROR_ERROR_BRANCH, taskDispatcherId: 'onError_1'},
                    task: onErrorTask,
                    updatedSubtasks,
                }).parameters
            ).toEqual({
                [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: updatedSubtasks,
                [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: onErrorTask.parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH],
            });
        });
    });

    describe('parallel', () => {
        const parallelConfig = TASK_DISPATCHER_CONFIG.parallel;

        it('extracts the dispatcher id and index from a placeholder id', () => {
            expect(parallelConfig.extractContextFromPlaceholder('parallel_1-parallel-placeholder-0')).toEqual({
                index: 0,
                taskDispatcherId: 'parallel_1',
            });
        });

        it('initializes an empty tasks array', () => {
            expect(parallelConfig.initializeParameters()).toEqual({tasks: []});
        });

        it('returns the parallel tasks from the task or the node', () => {
            const tasks = [makeTask('slack_1', 'slack/v1/send')];

            expect(parallelConfig.getSubtasks({task: makeTask('parallel_1', 'parallel/v1', {tasks})})).toBe(tasks);
            expect(parallelConfig.getSubtasks({node: makeNode({tasks})})).toBe(tasks);
            expect(parallelConfig.getSubtasks({task: makeTask('parallel_1', 'parallel/v1')})).toEqual([]);
            expect(parallelConfig.getSubtasks({task: makeTask('parallel_1', 'parallel/v1', {})})).toEqual([]);
        });

        it('replaces the tasks with the updated subtasks', () => {
            const updatedSubtasks = [makeTask('updated_1', 'slack/v1/send')];

            expect(
                parallelConfig.updateTaskParameters({
                    task: makeTask('parallel_1', 'parallel/v1', {tasks: []}),
                    updatedSubtasks,
                }).parameters
            ).toEqual({tasks: updatedSubtasks});
        });
    });

    describe.each(['loopBreak', 'subflow', 'terminate'] as const)('childless dispatcher %s', (dispatcherType) => {
        const config = TASK_DISPATCHER_CONFIG[dispatcherType];

        it('extracts the dispatcher id and index from a placeholder id', () => {
            expect(config.extractContextFromPlaceholder(`${dispatcherType}_1-placeholder-1`)).toEqual({
                index: 1,
                taskDispatcherId: `${dispatcherType}_1`,
            });
        });

        it('has no subtasks and empty initialized parameters', () => {
            expect(config.getSubtasks()).toEqual([]);
            expect(config.initializeParameters()).toEqual({});
            expect(config.getInitialParameters(defaultValueProperties)).toEqual({method: 'GET'});
        });

        it('returns the task unchanged on update', () => {
            const task = makeTask(`${dispatcherType}_1`, `${dispatcherType}/v1`, {value: 1});

            expect(config.updateTaskParameters({task, updatedSubtasks: [makeTask('slack_1', 'slack/v1/send')]})).toBe(
                task
            );
        });
    });
});

describe('getParentTaskDispatcherTask', () => {
    it('finds a condition parent through either case', () => {
        const conditionTask = makeTask('condition_1', 'condition/v1', {
            caseFalse: [makeTask('slack_2', 'slack/v1/send')],
            caseTrue: [makeTask('slack_1', 'slack/v1/send')],
        });

        const tasks = [makeTask('slack_0', 'slack/v1/send'), conditionTask];

        expect(getParentTaskDispatcherTask('slack_1', tasks)).toBe(conditionTask);
        expect(getParentTaskDispatcherTask('slack_2', tasks)).toBe(conditionTask);
    });

    it.each([
        {
            parameters: {cases: [{key: 'case_0', tasks: [makeTask('child_1', 'slack/v1/send')]}], default: []},
            type: 'branch/v1',
        },
        {parameters: {cases: [], default: [makeTask('child_1', 'slack/v1/send')]}, type: 'branch/v1'},
        {parameters: {iteratee: makeTask('child_1', 'slack/v1/send')}, type: 'each/v1'},
        {parameters: {branches: [[], [makeTask('child_1', 'slack/v1/send')]]}, type: 'fork-join/v1'},
        {parameters: {iteratee: [makeTask('child_1', 'slack/v1/send')]}, type: 'loop/v1'},
        {parameters: {iteratee: [makeTask('child_1', 'slack/v1/send')]}, type: 'map/v1'},
        {parameters: {[ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [makeTask('child_1', 'slack/v1/send')]}, type: 'on-error/v1'},
        {parameters: {tasks: [makeTask('child_1', 'slack/v1/send')]}, type: 'parallel/v1'},
    ])('finds a $type parent holding the subtask', ({parameters, type}) => {
        const dispatcherTask = makeTask('dispatcher_1', type, parameters);

        expect(getParentTaskDispatcherTask('child_1', [dispatcherTask])).toBe(dispatcherTask);
    });

    it('skips the task itself, non-dispatcher tasks and tasks without a type', () => {
        const selfLoop = makeTask('loop_1', 'loop/v1', {iteratee: [makeTask('loop_1', 'slack/v1/send')]});
        const nonDispatcher = makeTask('slack_1', 'slack/v1/send', {iteratee: [makeTask('child_1', 'slack/v1/send')]});
        const untypedTask = {
            name: 'untyped',
            parameters: {tasks: [makeTask('child_1', 'slack/v1/send')]},
        } as unknown as WorkflowTask;

        expect(getParentTaskDispatcherTask('loop_1', [selfLoop])).toBeUndefined();
        expect(getParentTaskDispatcherTask('child_1', [nonDispatcher, untypedTask])).toBeUndefined();
    });

    it('returns undefined when no dispatcher holds the task', () => {
        const loopTask = makeTask('loop_1', 'loop/v1', {iteratee: [makeTask('slack_1', 'slack/v1/send')]});

        expect(getParentTaskDispatcherTask('missing', [loopTask])).toBeUndefined();
        expect(getParentTaskDispatcherTask('slack_1', [])).toBeUndefined();
    });

    it('only inspects the direct subtasks of the given tasks', () => {
        const innerLoop = makeTask('loop_2', 'loop/v1', {iteratee: [makeTask('slack_1', 'slack/v1/send')]});
        const outerLoop = makeTask('loop_1', 'loop/v1', {iteratee: [innerLoop]});

        expect(getParentTaskDispatcherTask('loop_2', [outerLoop])).toBe(outerLoop);
        expect(getParentTaskDispatcherTask('slack_1', [outerLoop])).toBeUndefined();
        expect(getParentTaskDispatcherTask('slack_1', outerLoop.parameters?.iteratee)).toBe(innerLoop);
    });
});

describe('getTaskDispatcherTask', () => {
    const deepTask = makeTask('slack_deep', 'slack/v1/send');

    const nestedWorkflowTasks = [
        makeTask('slack_1', 'slack/v1/send'),
        makeTask('condition_1', 'condition/v1', {
            caseFalse: [],
            caseTrue: [
                makeTask('loop_1', 'loop/v1', {
                    iteratee: [
                        makeTask('branch_1', 'branch/v1', {
                            cases: [
                                {
                                    key: 'case_0',
                                    tasks: [
                                        makeTask('forkJoin_1', 'fork-join/v1', {
                                            branches: [
                                                [],
                                                [
                                                    makeTask('each_1', 'each/v1', {
                                                        iteratee: makeTask('parallel_1', 'parallel/v1', {
                                                            tasks: [
                                                                makeTask('onError_1', 'on-error/v1', {
                                                                    [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [deepTask],
                                                                    [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [],
                                                                }),
                                                            ],
                                                        }),
                                                    }),
                                                ],
                                            ],
                                        }),
                                    ],
                                },
                            ],
                            default: [],
                        }),
                    ],
                }),
            ],
        }),
    ];

    it('finds a top-level task by name', () => {
        expect(getTaskDispatcherTask({taskDispatcherId: 'slack_1', tasks: nestedWorkflowTasks})).toBe(
            nestedWorkflowTasks[0]
        );
    });

    it.each(['condition_1', 'loop_1', 'branch_1', 'forkJoin_1', 'each_1', 'parallel_1', 'onError_1', 'slack_deep'])(
        'finds %s through every nested dispatcher collection',
        (taskName) => {
            expect(getTaskDispatcherTask({taskDispatcherId: taskName, tasks: nestedWorkflowTasks})?.name).toBe(
                taskName
            );
        }
    );

    it('returns the deep task instance itself', () => {
        expect(getTaskDispatcherTask({taskDispatcherId: 'slack_deep', tasks: nestedWorkflowTasks})).toBe(deepTask);
    });

    it('finds tasks in the branch default collection and the on-error main branch', () => {
        const tasks = [
            makeTask('branch_1', 'branch/v1', {
                cases: [],
                default: [
                    makeTask('onError_1', 'on-error/v1', {
                        [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [makeTask('slack_main', 'slack/v1/send')],
                    }),
                ],
            }),
        ];

        expect(getTaskDispatcherTask({taskDispatcherId: 'slack_main', tasks})?.name).toBe('slack_main');
    });

    it('returns undefined for unknown names and tolerates null or non-object iteratees', () => {
        const tasks = [
            makeTask('each_1', 'each/v1', {iteratee: null}),
            makeTask('loop_1', 'loop/v1', {iteratee: 'not-a-task'}),
            makeTask('slack_1', 'slack/v1/send'),
        ];

        expect(getTaskDispatcherTask({taskDispatcherId: 'missing', tasks})).toBeUndefined();
        expect(getTaskDispatcherTask({taskDispatcherId: 'missing', tasks: []})).toBeUndefined();
    });

    it('does not look into collections that are not dispatcher subtask collections', () => {
        const tasks = [makeTask('slack_1', 'slack/v1/send', {items: [makeTask('hidden_1', 'slack/v1/send')]})];

        expect(getTaskDispatcherTask({taskDispatcherId: 'hidden_1', tasks})).toBeUndefined();
    });
});
