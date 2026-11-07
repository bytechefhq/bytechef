import {FINAL_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import placeAppendedTaskAfterPinnedPredecessor from './placeAppendedTaskAfterPinnedPredecessor';

function task(name: string, nodePosition?: {x: number; y: number}): WorkflowTask {
    return {
        metadata: nodePosition ? {ui: {nodePosition}} : undefined,
        name,
        parameters: {},
        type: 'test/v1/action',
    };
}

function trailingPlaceholder(position: {x: number; y: number}, measured?: {height: number; width: number}): Node {
    return {data: {}, id: FINAL_PLACEHOLDER_NODE_ID, measured, position, type: 'placeholder'};
}

describe('placeAppendedTaskAfterPinnedPredecessor', () => {
    it('pins the appended task where the carried trailing placeholder was drawn (LR)', () => {
        const tasks = [task('first'), task('pinned', {x: 3000, y: 500}), task('appended')];
        const canvasNodes = [trailingPlaceholder({x: 3400, y: 536}, {height: 28, width: 72})];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes,
            crossAxisShift: 0,
            direction: 'LR',
            tasks,
        });

        expect(placed[2].metadata?.ui?.nodePosition).toEqual({x: 3400, y: 514});
        expect(placed[1]).toBe(tasks[1]);
    });

    it('pins the appended task where the carried trailing placeholder was drawn (TB)', () => {
        const tasks = [task('pinned', {x: 500, y: 3000}), task('appended')];
        const canvasNodes = [trailingPlaceholder({x: 584, y: 3400}, {height: 28, width: 72})];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes,
            crossAxisShift: 0,
            direction: 'TB',
            tasks,
        });

        expect(placed[1].metadata?.ui?.nodePosition).toEqual({x: 500, y: 3400});
    });

    it('removes the cross-axis shift from the stored position', () => {
        const tasks = [task('pinned', {x: 3000, y: 500}), task('appended')];
        const canvasNodes = [trailingPlaceholder({x: 3400, y: 536}, {height: 28, width: 72})];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes,
            crossAxisShift: 40,
            direction: 'LR',
            tasks,
        });

        expect(placed[1].metadata?.ui?.nodePosition).toEqual({x: 3400, y: 474});
    });

    it('falls back to the placeholder constants when the chip has not been measured', () => {
        const tasks = [task('pinned', {x: 3000, y: 500}), task('appended')];
        const canvasNodes = [trailingPlaceholder({x: 3400, y: 536})];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes,
            crossAxisShift: 0,
            direction: 'LR',
            tasks,
        });

        expect(placed[1].metadata?.ui?.nodePosition).toEqual({x: 3400, y: 514});
    });

    it('leaves the appended task unpinned when the predecessor is not pinned', () => {
        const tasks = [task('first'), task('appended')];
        const canvasNodes = [trailingPlaceholder({x: 3400, y: 536})];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes,
            crossAxisShift: 0,
            direction: 'LR',
            tasks,
        });

        expect(placed).toBe(tasks);
    });

    it('leaves the appended task unpinned when there is no trailing placeholder on the canvas', () => {
        const tasks = [task('pinned', {x: 3000, y: 500}), task('appended')];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes: [],
            crossAxisShift: 0,
            direction: 'LR',
            tasks,
        });

        expect(placed).toBe(tasks);
    });

    it('leaves a lone first task unpinned', () => {
        const tasks = [task('appended')];

        const placed = placeAppendedTaskAfterPinnedPredecessor({
            canvasNodes: [trailingPlaceholder({x: 0, y: 0})],
            crossAxisShift: 0,
            direction: 'LR',
            tasks,
        });

        expect(placed).toBe(tasks);
    });
});
