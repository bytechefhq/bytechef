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

        expect(getTasksStructuralFingerprint(withElements)).not.toBe(
            getTasksStructuralFingerprint(withoutElements)
        );
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

        expect(getTasksStructuralFingerprint(withNullValues)).toBe(
            getTasksStructuralFingerprint(withoutElements)
        );
    });
});
