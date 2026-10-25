import {describe, expect, it} from 'vitest';

import {getDisabledNodeReferences} from './getDisabledNodeReferences';

describe('getDisabledNodeReferences', () => {
    it('finds a disabled task referenced inside an expression', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        expect(getDisabledNodeReferences({value: '${skippedTask.field}'}, disabledTaskNames)).toEqual(['skippedTask']);
    });

    it('does not match a name that is only a substring of a longer identifier', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        expect(getDisabledNodeReferences({value: '${skippedTask2.field}'}, disabledTaskNames)).toEqual([]);
    });

    it('finds a reference nested inside an array/object', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        const parameters = {
            list: [{nested: {value: '${skippedTask.field}'}}],
        };

        expect(getDisabledNodeReferences(parameters, disabledTaskNames)).toEqual(['skippedTask']);
    });

    it('finds a disabled task referenced by a formula without an expression wrapper', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        expect(getDisabledNodeReferences({value: '=skippedTask.body'}, disabledTaskNames)).toEqual(['skippedTask']);
        expect(getDisabledNodeReferences({value: "=concat('prefix', skippedTask.body)"}, disabledTaskNames)).toEqual([
            'skippedTask',
        ]);
    });

    it('ignores a disabled task name that appears only inside a formula string literal', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        expect(getDisabledNodeReferences({value: "=concat('skippedTask', 'x')"}, disabledTaskNames)).toEqual([]);
    });

    it('ignores a plain-text mention outside an expression', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        expect(getDisabledNodeReferences({value: 'See skippedTask for details'}, disabledTaskNames)).toEqual([]);
    });

    it('reports each referenced name once even when referenced multiple times', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        const parameters = {
            a: '${skippedTask.field}',
            b: '${skippedTask.other}',
        };

        expect(getDisabledNodeReferences(parameters, disabledTaskNames)).toEqual(['skippedTask']);
    });

    it('escapes regex metacharacters in disabled task names', () => {
        const disabledTaskNames = new Set(['task.name+1']);

        expect(getDisabledNodeReferences({value: '${task.name+1.field}'}, disabledTaskNames)).toEqual(['task.name+1']);
    });

    it('does not attribute a nested task reference to its dispatcher ancestor', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        const nestedTask = {
            name: 'true_1',
            parameters: {value: '${skippedTask.field}'},
            type: 'test/v1/action',
        };

        const conditionParameters = {
            caseTrue: [nestedTask],
        };

        expect(getDisabledNodeReferences(conditionParameters, disabledTaskNames)).toEqual([]);

        expect(getDisabledNodeReferences(nestedTask.parameters, disabledTaskNames)).toEqual(['skippedTask']);
    });

    it('still scans a branch case own fields while skipping its nested tasks', () => {
        const disabledTaskNames = new Set(['skippedTask']);

        const parameters = {
            cases: [
                {
                    key: '${skippedTask.field}',
                    tasks: [{name: 'case_task', parameters: {}, type: 'test/v1/action'}],
                },
            ],
        };

        expect(getDisabledNodeReferences(parameters, disabledTaskNames)).toEqual(['skippedTask']);
    });
});
