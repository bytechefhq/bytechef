import {describe, expect, it} from 'vitest';

import {getStructureKeyCompletions, getStructureValueCompletions} from './workflowStructureCompletions';

const labelsOf = (containerPath: string[], existingKeys: string[] = []) =>
    getStructureKeyCompletions(containerPath, existingKeys)?.map((item) => item.label);

describe('getStructureKeyCompletions', () => {
    it('offers the workflow keys at the root, leaving out the ones already present', () => {
        expect(labelsOf([], ['label', 'tasks'])).toEqual([
            'description',
            'inputs',
            'outputs',
            'triggers',
            'maxRetries',
        ]);
    });

    it('offers the keys of inputs, outputs and triggers', () => {
        expect(labelsOf(['inputs', '0'])).toEqual(['name', 'label', 'type', 'required']);
        expect(labelsOf(['outputs', '1'])).toEqual(['name', 'value']);
        expect(labelsOf(['triggers', '0'], ['name', 'type'])).toEqual([
            'label',
            'description',
            'parameters',
            'timeout',
            'disabled',
            'metadata',
        ]);
    });

    it('offers the task keys for tasks and for tasks nested in pre, post and finalize', () => {
        const taskKeys = labelsOf(['tasks', '2']);

        expect(taskKeys).toContain('parameters');
        expect(taskKeys).toContain('finalize');
        expect(labelsOf(['tasks', '0', 'pre', '1'])).toEqual(taskKeys);
        expect(labelsOf(['tasks', '0', 'finalize', '0', 'post', '0'])).toEqual(taskKeys);
    });

    it('leaves parameters, metadata and unknown containers to other completions', () => {
        expect(labelsOf(['tasks', '0', 'parameters'])).toBeUndefined();
        expect(labelsOf(['tasks', '0', 'parameters', 'iteratee', '0'])).toBeUndefined();
        expect(labelsOf(['tasks', '0', 'metadata'])).toBeUndefined();
        expect(labelsOf(['inputs'])).toBeUndefined();
        expect(labelsOf(['custom', '0'])).toBeUndefined();
    });
});

describe('getStructureValueCompletions', () => {
    it('offers the input types and boolean flags', () => {
        const types = getStructureValueCompletions(['inputs', '0', 'type'], 2, (value) => String(value));

        expect(types?.map((item) => item.label)).toEqual([
            'boolean',
            'date',
            'date_time',
            'integer',
            'number',
            'string',
            'time',
        ]);
        expect(types?.[0]).toMatchObject({kind: 'value', replaceLength: 2});

        expect(
            getStructureValueCompletions(['tasks', '0', 'pre', '0', 'disabled'], 0, (value) =>
                JSON.stringify(value)
            )?.map((item) => item.insertText)
        ).toEqual(['true', 'false']);
    });

    it('leaves task types and parameter values to other completions', () => {
        expect(getStructureValueCompletions(['tasks', '0', 'type'], 0, String)).toBeUndefined();
        expect(getStructureValueCompletions(['tasks', '0', 'parameters', 'required'], 0, String)).toBeUndefined();
    });
});
