import {describe, expect, it} from 'vitest';

import {buildFocusPath} from '../buildFocusPath';

describe('buildFocusPath', () => {
    it('expands iteration elements into accordion values', () => {
        expect(
            buildFocusPath([{taskExecutionId: 1}, {iterationIndex: 2, taskExecutionId: 7}, {taskExecutionId: 9}])
        ).toEqual(['1', '1-iteration-2', '7', '9']);
    });

    it('returns an empty path for an empty focus path', () => {
        expect(buildFocusPath([])).toEqual([]);
    });
});
