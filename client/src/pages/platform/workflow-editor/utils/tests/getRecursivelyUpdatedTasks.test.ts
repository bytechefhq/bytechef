import {ON_ERROR_WIRE_KEY_ERROR_BRANCH, ON_ERROR_WIRE_KEY_MAIN_BRANCH} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getRecursivelyUpdatedTasks from '../getRecursivelyUpdatedTasks';

function makeTask(name: string, type: string, parameters?: WorkflowTask['parameters']): WorkflowTask {
    return {name, parameters, type};
}

const replacementTask = makeTask('target_1', 'slack/v1/send', {text: 'updated'});

function makeTarget() {
    return makeTask('target_1', 'slack/v1/send', {text: 'original'});
}

describe('getRecursivelyUpdatedTasks', () => {
    it('wraps a non-array input in an array', () => {
        const singleTask = makeTask('slack_1', 'slack/v1/send');

        expect(getRecursivelyUpdatedTasks(singleTask as unknown as Array<WorkflowTask>, replacementTask)).toEqual([
            singleTask,
        ]);
    });

    it('replaces a matching top-level task and keeps the others by reference', () => {
        const otherTask = makeTask('slack_1', 'slack/v1/send', {text: 'other'});

        const updatedTasks = getRecursivelyUpdatedTasks([otherTask, makeTarget()], replacementTask);

        expect(updatedTasks[0]).toBe(otherTask);
        expect(updatedTasks[1]).toBe(replacementTask);
    });

    it('returns the tasks unchanged when nothing matches', () => {
        const tasks = [makeTask('slack_1', 'slack/v1/send'), makeTask('loop_1', 'loop/v1', {iteratee: []})];

        expect(getRecursivelyUpdatedTasks(tasks, replacementTask)).toEqual(tasks);
    });

    it('replaces a task inside either condition case', () => {
        const conditionTask = makeTask('condition_1', 'condition/v1', {
            caseFalse: [makeTask('slack_2', 'slack/v1/send')],
            caseTrue: [makeTarget()],
        });

        const [updatedCondition] = getRecursivelyUpdatedTasks([conditionTask], replacementTask);

        expect(updatedCondition.parameters?.caseTrue).toEqual([replacementTask]);
        expect(updatedCondition.parameters?.caseFalse).toEqual(conditionTask.parameters?.caseFalse);
        expect(conditionTask.parameters?.caseTrue[0].parameters.text).toBe('original');

        const falseCaseCondition = makeTask('condition_1', 'condition/v1', {caseFalse: [makeTarget()]});

        const [updatedFalseCaseCondition] = getRecursivelyUpdatedTasks([falseCaseCondition], replacementTask);

        expect(updatedFalseCaseCondition.parameters).toEqual({caseFalse: [replacementTask]});
    });

    it('replaces a task inside either on-error branch without adding the missing branch', () => {
        const mainBranchTask = makeTask('onError_1', 'on-error/v1', {[ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [makeTarget()]});
        const errorBranchTask = makeTask('onError_2', 'on-error/v1', {
            [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [makeTarget()],
            [ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [makeTask('slack_1', 'slack/v1/send')],
        });

        const [updatedMainBranchTask, updatedErrorBranchTask] = getRecursivelyUpdatedTasks(
            [mainBranchTask, errorBranchTask],
            replacementTask
        );

        expect(updatedMainBranchTask.parameters).toEqual({[ON_ERROR_WIRE_KEY_MAIN_BRANCH]: [replacementTask]});
        expect(updatedErrorBranchTask.parameters?.[ON_ERROR_WIRE_KEY_ERROR_BRANCH]).toEqual([replacementTask]);
        expect(updatedErrorBranchTask.parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH]).toEqual(
            errorBranchTask.parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH]
        );
    });

    it('replaces a task inside an array iteratee (loop / map)', () => {
        const loopTask = makeTask('loop_1', 'loop/v1', {
            items: [1, 2],
            iteratee: [makeTask('slack_1', 'slack/v1/send'), makeTarget()],
        });

        const [updatedLoop] = getRecursivelyUpdatedTasks([loopTask], replacementTask);

        expect(updatedLoop.parameters?.items).toEqual([1, 2]);
        expect(updatedLoop.parameters?.iteratee[1]).toBe(replacementTask);
    });

    it('replaces an object iteratee (each) and keeps it an object', () => {
        const eachTask = makeTask('each_1', 'each/v1', {iteratee: makeTarget()});

        const [updatedEach] = getRecursivelyUpdatedTasks([eachTask], replacementTask);

        expect(updatedEach.parameters?.iteratee).toBe(replacementTask);
    });

    it('replaces a task nested inside an object iteratee', () => {
        const eachTask = makeTask('each_1', 'each/v1', {
            iteratee: makeTask('loop_1', 'loop/v1', {iteratee: [makeTarget()]}),
        });

        const [updatedEach] = getRecursivelyUpdatedTasks([eachTask], replacementTask);

        expect(updatedEach.parameters?.iteratee.parameters.iteratee).toEqual([replacementTask]);
    });

    it('replaces tasks in branch cases and default', () => {
        const branchTask = makeTask('branch_1', 'branch/v1', {
            cases: [
                {key: 'case_0', tasks: [makeTarget()]},
                {key: 'case_1', tasks: [makeTask('slack_1', 'slack/v1/send')]},
                {key: 'case_2'},
            ],
            default: [makeTask('slack_2', 'slack/v1/send')],
        });

        const [updatedBranch] = getRecursivelyUpdatedTasks([branchTask], replacementTask);

        expect(updatedBranch.parameters?.cases).toEqual([
            {key: 'case_0', tasks: [replacementTask]},
            {key: 'case_1', tasks: [makeTask('slack_1', 'slack/v1/send')]},
            {key: 'case_2', tasks: undefined},
        ]);
        expect(updatedBranch.parameters?.default).toEqual(branchTask.parameters?.default);

        const defaultBranchTask = makeTask('branch_2', 'branch/v1', {cases: [], default: [makeTarget()]});

        const [updatedDefaultBranch] = getRecursivelyUpdatedTasks([defaultBranchTask], replacementTask);

        expect(updatedDefaultBranch.parameters).toEqual({cases: [], default: [replacementTask]});
    });

    it('does not add a default collection to a branch that has none', () => {
        const branchTask = makeTask('branch_1', 'branch/v1', {cases: [{key: 'case_0', tasks: [makeTarget()]}]});

        const [updatedBranch] = getRecursivelyUpdatedTasks([branchTask], replacementTask);

        expect(updatedBranch.parameters).not.toHaveProperty('default');
    });

    it('replaces a task inside parallel tasks', () => {
        const parallelTask = makeTask('parallel_1', 'parallel/v1', {tasks: [makeTarget()]});

        const [updatedParallel] = getRecursivelyUpdatedTasks([parallelTask], replacementTask);

        expect(updatedParallel.parameters?.tasks).toEqual([replacementTask]);
    });

    it('replaces a task inside a fork-join branch', () => {
        const forkJoinTask = makeTask('forkJoin_1', 'fork-join/v1', {
            branches: [[makeTask('slack_1', 'slack/v1/send')], [makeTarget()]],
        });

        const [updatedForkJoin] = getRecursivelyUpdatedTasks([forkJoinTask], replacementTask);

        expect(updatedForkJoin.parameters?.branches).toEqual([
            [makeTask('slack_1', 'slack/v1/send')],
            [replacementTask],
        ]);
    });

    it('replaces a task nested several dispatchers deep', () => {
        const tasks = [
            makeTask('condition_1', 'condition/v1', {
                caseFalse: [],
                caseTrue: [
                    makeTask('branch_1', 'branch/v1', {
                        cases: [
                            {
                                key: 'case_0',
                                tasks: [
                                    makeTask('forkJoin_1', 'fork-join/v1', {
                                        branches: [
                                            [
                                                makeTask('parallel_1', 'parallel/v1', {
                                                    tasks: [
                                                        makeTask('onError_1', 'on-error/v1', {
                                                            [ON_ERROR_WIRE_KEY_ERROR_BRANCH]: [makeTarget()],
                                                        }),
                                                    ],
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
        ];

        const [updatedCondition] = getRecursivelyUpdatedTasks(tasks, replacementTask);

        const deepTask =
            updatedCondition.parameters?.caseTrue[0].parameters.cases[0].tasks[0].parameters.branches[0][0].parameters
                .tasks[0].parameters[ON_ERROR_WIRE_KEY_ERROR_BRANCH][0];

        expect(deepTask).toBe(replacementTask);
        expect(JSON.stringify(tasks)).toContain('"original"');
    });

    it('replaces a dispatcher as a whole instead of descending into it', () => {
        const loopTask = makeTask('target_1', 'loop/v1', {iteratee: [makeTask('target_1', 'slack/v1/send')]});

        expect(getRecursivelyUpdatedTasks([loopTask], replacementTask)).toEqual([replacementTask]);
    });
});
