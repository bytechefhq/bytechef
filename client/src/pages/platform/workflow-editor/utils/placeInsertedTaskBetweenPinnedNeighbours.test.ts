import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import placeInsertedTaskBetweenPinnedNeighbours from './placeInsertedTaskBetweenPinnedNeighbours';

function task(name: string, nodePosition?: {x?: number; y?: number}): WorkflowTask {
    return {
        metadata: nodePosition ? {ui: {nodePosition: nodePosition as {x: number; y: number}}} : undefined,
        name,
        parameters: {},
        type: 'test/v1/action',
    };
}

describe('placeInsertedTaskBetweenPinnedNeighbours', () => {
    it('pins the inserted task at the midpoint of two pinned neighbours', () => {
        const tasks = [task('first', {x: 100, y: 600}), task('inserted'), task('third', {x: 500, y: 800})];

        const placed = placeInsertedTaskBetweenPinnedNeighbours(tasks, 1);

        expect(placed[1].metadata?.ui?.nodePosition).toEqual({x: 300, y: 700});
        expect(placed[0]).toBe(tasks[0]);
        expect(placed[2]).toBe(tasks[2]);
    });

    it('keeps the inserted task unpinned when the predecessor is not pinned', () => {
        const tasks = [task('first'), task('inserted'), task('third', {x: 500, y: 800})];

        expect(placeInsertedTaskBetweenPinnedNeighbours(tasks, 1)[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('keeps the inserted task unpinned when the successor is not pinned', () => {
        const tasks = [task('first', {x: 100, y: 600}), task('inserted'), task('third')];

        expect(placeInsertedTaskBetweenPinnedNeighbours(tasks, 1)[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('keeps the inserted task unpinned at either end of the list', () => {
        const atStart = [task('inserted'), task('second', {x: 100, y: 600})];
        const atEnd = [task('first', {x: 100, y: 600}), task('inserted')];

        expect(placeInsertedTaskBetweenPinnedNeighbours(atStart, 0)[0].metadata?.ui?.nodePosition).toBeUndefined();
        expect(placeInsertedTaskBetweenPinnedNeighbours(atEnd, 1)[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it('treats a half-defined neighbour position as unpinned', () => {
        const tasks = [task('first', {y: 600}), task('inserted'), task('third', {x: 500, y: 800})];

        expect(placeInsertedTaskBetweenPinnedNeighbours(tasks, 1)[1].metadata?.ui?.nodePosition).toBeUndefined();
    });

    it("preserves the inserted task's other ui metadata", () => {
        const inserted = {
            ...task('inserted'),
            metadata: {ui: {dynamicPropertyTypes: {field: 'STRING'}}},
        } as WorkflowTask;
        const tasks = [task('first', {x: 0, y: 0}), inserted, task('third', {x: 200, y: 100})];

        expect(placeInsertedTaskBetweenPinnedNeighbours(tasks, 1)[1].metadata?.ui).toEqual({
            dynamicPropertyTypes: {field: 'STRING'},
            nodePosition: {x: 100, y: 50},
        });
    });
});
