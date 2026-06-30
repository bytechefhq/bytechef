import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getDuplicateNodeNames from '../getDuplicateNodeNames';

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

    it('does not report a dispatcher whose nested subtask appears once in the flattened list', () => {
        const tasks = [
            task('loop_1', {parameters: {iteratee: [task('airtable_1')]}, type: 'loop/v1'}),
            task('airtable_1', {type: 'airtable/v1/customAction'}),
        ];

        expect(getDuplicateNodeNames(tasks)).toEqual([]);
    });

    it('detects a genuine duplicate between a nested subtask and a sibling task', () => {
        const tasks = [
            task('condition_1', {parameters: {caseFalse: [task('email_1')], caseTrue: []}, type: 'condition/v1'}),
            task('email_1', {type: 'email/v1/send'}),
            task('email_1', {type: 'email/v1/send'}),
        ];

        expect(getDuplicateNodeNames(tasks)).toEqual(['email_1']);
    });

    it('detects a collision between a trigger name and a task name', () => {
        const triggers = [{name: 'manual_1', type: 'manual/v1/manual'} as WorkflowTrigger];
        const tasks = [task('manual_1')];

        expect(getDuplicateNodeNames(tasks, triggers)).toEqual(['manual_1']);
    });
});
