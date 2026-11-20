import {
    getTopLevelTaskNames,
    isCrossAxisOverflowing,
    isWorkflowLaidOut,
} from '@/pages/platform/workflow-editor/utils/initialFitViewportUtils';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

const toNode = (id: string): Node => ({data: {}, id, position: {x: 0, y: 0}});

describe('isCrossAxisOverflowing', () => {
    it('detects a top-to-bottom flow spilling past the left edge', () => {
        expect(
            isCrossAxisOverflowing({
                bounds: {height: 400, width: 600, x: -200, y: 0},
                flowHeight: 800,
                flowWidth: 1200,
                layoutDirection: 'TB',
                viewport: {x: 100, y: 0, zoom: 1},
            })
        ).toBe(true);
    });

    it('detects a top-to-bottom flow spilling past the right edge', () => {
        expect(
            isCrossAxisOverflowing({
                bounds: {height: 400, width: 1300, x: 0, y: 0},
                flowHeight: 800,
                flowWidth: 1200,
                layoutDirection: 'TB',
                viewport: {x: 0, y: 0, zoom: 1},
            })
        ).toBe(true);
    });

    it('ignores a top-to-bottom flow that is only taller than the canvas', () => {
        expect(
            isCrossAxisOverflowing({
                bounds: {height: 5000, width: 600, x: 0, y: 0},
                flowHeight: 800,
                flowWidth: 1200,
                layoutDirection: 'TB',
                viewport: {x: 100, y: 25, zoom: 1},
            })
        ).toBe(false);
    });

    it('detects a left-to-right flow spilling past the bottom edge', () => {
        expect(
            isCrossAxisOverflowing({
                bounds: {height: 900, width: 600, x: 0, y: 0},
                flowHeight: 800,
                flowWidth: 1200,
                layoutDirection: 'LR',
                viewport: {x: 0, y: 0, zoom: 1},
            })
        ).toBe(true);
    });

    it('ignores a left-to-right flow that is only wider than the canvas', () => {
        expect(
            isCrossAxisOverflowing({
                bounds: {height: 400, width: 5000, x: 0, y: 0},
                flowHeight: 800,
                flowWidth: 1200,
                layoutDirection: 'LR',
                viewport: {x: 0, y: 100, zoom: 1},
            })
        ).toBe(false);
    });
});

describe('getTopLevelTaskNames', () => {
    it('reads the names of the top-level tasks', () => {
        expect(getTopLevelTaskNames(JSON.stringify({tasks: [{name: 'condition_1'}, {name: 'aiAgent_5'}]}))).toEqual([
            'condition_1',
            'aiAgent_5',
        ]);
    });

    it('returns no names for a missing or broken definition', () => {
        expect(getTopLevelTaskNames(undefined)).toEqual([]);
        expect(getTopLevelTaskNames('{')).toEqual([]);
    });
});

describe('isWorkflowLaidOut', () => {
    it('waits for the first nodes', () => {
        expect(isWorkflowLaidOut([], [])).toBe(false);
    });

    it('waits until every top-level task has a node', () => {
        expect(isWorkflowLaidOut([toNode('trigger_1'), toNode('condition_1')], ['condition_1', 'aiAgent_5'])).toBe(
            false
        );
    });

    it('is laid out once every top-level task has a node', () => {
        expect(
            isWorkflowLaidOut(
                [toNode('trigger_1'), toNode('condition_1'), toNode('aiAgent_5')],
                ['condition_1', 'aiAgent_5']
            )
        ).toBe(true);
    });
});
