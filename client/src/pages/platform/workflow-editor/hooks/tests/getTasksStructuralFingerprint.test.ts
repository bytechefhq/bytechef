import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getTasksStructuralFingerprint} from '../useLayout';

function makeTask(overrides: Partial<WorkflowTask> & {name: string; type: string}): WorkflowTask {
    return overrides as WorkflowTask;
}

describe('getTasksStructuralFingerprint', () => {
    it('should produce the same fingerprint for tasks differing only in parameter values', () => {
        const tasksA = [makeTask({name: 'http_1', parameters: {url: 'http://a.com'}, type: 'httpClient/v1/get'})];
        const tasksB = [makeTask({name: 'http_1', parameters: {url: 'http://b.com'}, type: 'httpClient/v1/get'})];

        expect(getTasksStructuralFingerprint(tasksA)).toBe(getTasksStructuralFingerprint(tasksB));
    });

    it('should produce different fingerprints when task names differ', () => {
        const tasksA = [makeTask({name: 'http_1', type: 'httpClient/v1/get'})];
        const tasksB = [makeTask({name: 'http_2', type: 'httpClient/v1/get'})];

        expect(getTasksStructuralFingerprint(tasksA)).not.toBe(getTasksStructuralFingerprint(tasksB));
    });

    it('should produce different fingerprints when clusterRoot differs', () => {
        const tasksA = [makeTask({clusterRoot: true, name: 'ds_1', type: 'dataStream/v1/stream'})];
        const tasksB = [makeTask({clusterRoot: false, name: 'ds_1', type: 'dataStream/v1/stream'})];

        expect(getTasksStructuralFingerprint(tasksA)).not.toBe(getTasksStructuralFingerprint(tasksB));
    });

    it('should produce different fingerprints when clusterElements presence differs', () => {
        const withElements = [
            makeTask({
                clusterElements: {source: [{name: 'csv_1', type: 'csvFile/v1/read'}]} as Record<string, unknown>,
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];
        const withoutElements = [
            makeTask({
                clusterElements: {source: []} as Record<string, unknown>,
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];

        expect(getTasksStructuralFingerprint(withElements)).not.toBe(getTasksStructuralFingerprint(withoutElements));
    });

    it('should treat empty clusterElements the same as no clusterElements', () => {
        const withEmpty = [
            makeTask({
                clusterElements: {} as Record<string, unknown>,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];
        const withNone = [
            makeTask({
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];

        expect(getTasksStructuralFingerprint(withEmpty)).toBe(getTasksStructuralFingerprint(withNone));
    });

    it('should treat clusterElements with only null/empty-array values as not filled', () => {
        const withNullValues = [
            makeTask({
                clusterElements: {processor: null, sink: [], source: null} as unknown as Record<string, unknown>,
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];
        const withoutElements = [
            makeTask({
                clusterRoot: true,
                name: 'ds_1',
                type: 'dataStream/v1/stream',
            }),
        ];

        expect(getTasksStructuralFingerprint(withNullValues)).toBe(getTasksStructuralFingerprint(withoutElements));
    });
});

describe('graph layout signature', () => {
    const makeGraphTask = (
        transitions: Array<{condition?: string; from: string; to: string}>,
        positions: Record<string, {x: number; y: number}> = {},
        startNode = 'node_0'
    ) =>
        ({
            name: 'graph_1',
            parameters: {
                nodes: ['node_0', 'node_1'].map((name) => ({
                    name,
                    ...(positions[name] ? {metadata: {ui: {nodePosition: positions[name]}}} : {}),
                })),
                startNode,
                transitions,
            },
            type: 'graph/v1',
        }) as unknown as WorkflowTask;

    it('changes the fingerprint when a transition is added, retargeted or conditioned', () => {
        const none = getTasksStructuralFingerprint([makeGraphTask([])]);
        const added = getTasksStructuralFingerprint([makeGraphTask([{from: 'node_0', to: 'node_1'}])]);
        const retargeted = getTasksStructuralFingerprint([makeGraphTask([{from: 'node_0', to: 'node_2'}])]);
        const conditioned = getTasksStructuralFingerprint([
            makeGraphTask([{condition: '${node_0.ok}', from: 'node_0', to: 'node_2'}]),
        ]);

        expect(none).not.toBe(added);
        expect(added).not.toBe(retargeted);
        expect(retargeted).not.toBe(conditioned);
    });

    it('changes the fingerprint when a member moves, since the frame is sized from member positions', () => {
        const transitions = [{from: 'node_0', to: 'node_1'}];

        const unplaced = getTasksStructuralFingerprint([makeGraphTask(transitions)]);
        const placed = getTasksStructuralFingerprint([makeGraphTask(transitions, {node_1: {x: 300, y: 0}})]);
        const moved = getTasksStructuralFingerprint([makeGraphTask(transitions, {node_1: {x: 300, y: 120}})]);

        expect(unplaced).not.toBe(placed);
        expect(placed).not.toBe(moved);
    });

    // Re-pointing the Start pill writes `startNode` and nothing else, so this is the only thing
    // that can tell the layout to re-emit the `graphStart` edge. Without it the canvas keeps
    // asserting the graph enters where it used to, while the saved workflow enters somewhere else.
    it('changes the fingerprint when the start node is re-pointed', () => {
        const transitions = [{from: 'node_0', to: 'node_1'}];
        const positions = {node_0: {x: 24, y: 0}, node_1: {x: 300, y: 0}};

        expect(getTasksStructuralFingerprint([makeGraphTask(transitions, positions, 'node_0')])).not.toBe(
            getTasksStructuralFingerprint([makeGraphTask(transitions, positions, 'node_1')])
        );
    });

    it('keeps the fingerprint stable when nothing structural changed', () => {
        const transitions = [{from: 'node_0', to: 'node_1'}];
        const positions = {node_1: {x: 300, y: 0}};

        expect(getTasksStructuralFingerprint([makeGraphTask(transitions, positions)])).toBe(
            getTasksStructuralFingerprint([makeGraphTask(transitions, positions)])
        );
    });
});
