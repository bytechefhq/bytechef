import {describe, expect, it} from 'vitest';

import {buildFocusPath} from '../buildFocusPath';

describe('buildFocusPath', () => {
    it('expands iteration elements into accordion values', () => {
        expect(
            buildFocusPath([{taskExecutionId: '1'}, {iterationIndex: 2, taskExecutionId: '7'}, {taskExecutionId: '9'}])
        ).toEqual(['1', '1-iteration-2', '7', '9']);
    });

    it('returns an empty path for an empty focus path', () => {
        expect(buildFocusPath([])).toEqual([]);
    });

    it('expands the first iteration of a loop, whose index is 0', () => {
        expect(buildFocusPath([{taskExecutionId: '1'}, {iterationIndex: 0, taskExecutionId: '7'}])).toEqual([
            '1',
            '1-iteration-0',
            '7',
        ]);
    });

    it('expands the try branch of an on-error task, whose index is 0, and the error branch', () => {
        expect(buildFocusPath([{taskExecutionId: '3'}, {iterationIndex: 0, taskExecutionId: '4'}])).toEqual([
            '3',
            '3-iteration-0',
            '4',
        ]);
        expect(buildFocusPath([{taskExecutionId: '3'}, {iterationIndex: 1, taskExecutionId: '5'}])).toEqual([
            '3',
            '3-iteration-1',
            '5',
        ]);
    });

    it('ignores an iteration index on the first element, which has no parent', () => {
        expect(buildFocusPath([{iterationIndex: 0, taskExecutionId: '1'}, {taskExecutionId: '9'}])).toEqual(['1', '9']);
    });

    it('does not expand an iteration for children of tasks that are not loops', () => {
        expect(buildFocusPath([{taskExecutionId: '1'}, {childIndex: 0, taskExecutionId: '9'}])).toEqual(['1', '9']);
    });
});
