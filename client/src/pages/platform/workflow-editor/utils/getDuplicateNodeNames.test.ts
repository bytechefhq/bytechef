import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getDuplicateNodeNames from './getDuplicateNodeNames';

const task = (name: string, partial: Partial<WorkflowTask> = {}): WorkflowTask =>
    ({name, type: 'example/v1/action', ...partial}) as WorkflowTask;

describe('getDuplicateNodeNames', () => {
    it('returns an empty array when all names are unique', () => {
        const tasks = [task('liferay_1'), task('condition_1'), task('liferay_2')];

        expect(getDuplicateNodeNames(tasks)).toEqual([]);
    });

    it('returns an empty array for undefined input', () => {
        expect(getDuplicateNodeNames()).toEqual([]);
    });

    it('detects duplicate top-level task names', () => {
        const tasks = [task('condition_2'), task('liferay_3'), task('condition_2')];

        expect(getDuplicateNodeNames(tasks)).toEqual(['condition_2']);
    });

    it('detects a task name duplicated inside a condition branch', () => {
        const tasks = [
            task('condition_1', {
                parameters: {caseFalse: [task('email_1')], caseTrue: []},
                type: 'condition/v1',
            }),
            task('email_1'),
        ];

        expect(getDuplicateNodeNames(tasks)).toEqual(['email_1']);
    });

    it('detects names duplicated across two separate condition branches', () => {
        // Mirrors the reported workflow: two condition tasks both named
        // "condition_2", each nesting an email task named "email_1".
        const tasks: Array<WorkflowTask> = [
            task('liferay_2', {type: 'liferay/v1/headlessRequest'}),
            task('condition_2', {
                parameters: {caseFalse: [task('email_1', {type: 'email/v1/send'})], caseTrue: []},
                type: 'condition/v1',
            }),
            task('liferay_3', {type: 'liferay/v1/headlessRequest'}),
            task('condition_2', {
                parameters: {caseFalse: [task('email_1', {type: 'email/v1/send'})], caseTrue: []},
                type: 'condition/v1',
            }),
        ];

        expect(getDuplicateNodeNames(tasks).sort()).toEqual(['condition_2', 'email_1']);
    });

    it('detects a collision between a trigger name and a task name', () => {
        const triggers = [{name: 'manual_1', type: 'manual/v1/manual'} as WorkflowTrigger];
        const tasks = [task('manual_1')];

        expect(getDuplicateNodeNames(tasks, triggers)).toEqual(['manual_1']);
    });

    it('detects duplicates nested inside loop, branch and parallel dispatchers', () => {
        const tasks: Array<WorkflowTask> = [
            task('loop_1', {parameters: {iteratee: [task('shared')]}, type: 'loop/v1'}),
            task('branch_1', {
                parameters: {cases: [{key: 'a', tasks: [task('shared')]}]},
                type: 'branch/v1',
            }),
            task('parallel_1', {parameters: {tasks: [task('parallel_child')]}, type: 'parallel/v1'}),
        ];

        expect(getDuplicateNodeNames(tasks)).toEqual(['shared']);
    });
});
