import {describe, expect, it} from 'vitest';

import compareWorkflowLabels from '../compareWorkflowLabels';

describe('compareWorkflowLabels', () => {
    it('orders labels alphabetically regardless of case', () => {
        expect(['Setup leads sheet', 'Connection check', 'archive old leads'].sort(compareWorkflowLabels)).toEqual([
            'archive old leads',
            'Connection check',
            'Setup leads sheet',
        ]);
    });

    it('orders a missing label before any label', () => {
        expect(compareWorkflowLabels(undefined, 'archive')).toBeLessThan(0);
        expect(compareWorkflowLabels('archive', undefined)).toBeGreaterThan(0);
        expect(compareWorkflowLabels(undefined, undefined)).toBe(0);
    });
});
