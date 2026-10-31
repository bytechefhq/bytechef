import {WorkflowNodeChangeKind} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import getChangeHighlightBorderClassName from './getChangeHighlightBorderClassName';

describe('getChangeHighlightBorderClassName', () => {
    it('maps every change kind to its border colour', () => {
        expect(getChangeHighlightBorderClassName(WorkflowNodeChangeKind.Added)).toBe(
            'border-green-500 hover:border-green-500'
        );
        expect(getChangeHighlightBorderClassName(WorkflowNodeChangeKind.Modified)).toBe(
            'border-amber-500 hover:border-amber-500'
        );
        expect(getChangeHighlightBorderClassName(WorkflowNodeChangeKind.Moved)).toBe(
            'border-blue-500 hover:border-blue-500'
        );
        expect(getChangeHighlightBorderClassName(WorkflowNodeChangeKind.Removed)).toBe(
            'border-red-500 hover:border-red-500'
        );
    });

    it('returns undefined without a kind', () => {
        expect(getChangeHighlightBorderClassName()).toBeUndefined();
    });
});
