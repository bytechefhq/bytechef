import {describe, expect, it} from 'vitest';

import {appendDataPillPathSegment, buildDataPillPath, getDataPillSampleValue} from '../dataPillPath';

describe('dataPillPath', () => {
    describe('appendDataPillPathSegment', () => {
        it('joins an identifier with a dot', () => {
            expect(appendDataPillPathSegment('node_1', 'user')).toBe('node_1.user');
        });

        it('appends the array index placeholder without a dot', () => {
            expect(appendDataPillPathSegment('node_1.tags', '[index]')).toBe('node_1.tags[index]');
        });

        it('quotes a key with a hyphen or a leading digit', () => {
            expect(appendDataPillPathSegment('node_1.user', 'first-name')).toBe("node_1.user['first-name']");
            expect(appendDataPillPathSegment('node_1', '4broj telefona')).toBe("node_1['4broj telefona']");
        });

        it('quotes an output file key, keeping its dots and slashes inside the key', () => {
            expect(appendDataPillPathSegment('produceReport.outputFiles', 'report.csv')).toBe(
                "produceReport.outputFiles['report.csv']"
            );
            expect(appendDataPillPathSegment('produceReport.outputFiles', 'charts/summary.txt')).toBe(
                "produceReport.outputFiles['charts/summary.txt']"
            );
        });
    });

    describe('buildDataPillPath', () => {
        it('builds a reference from the node name and each key', () => {
            expect(buildDataPillPath('produceReport', ['outputFiles', 'charts/summary.txt', 'name'])).toBe(
                "produceReport.outputFiles['charts/summary.txt'].name"
            );
        });

        it('returns the node name alone for no keys', () => {
            expect(buildDataPillPath('node_1', [])).toBe('node_1');
        });
    });

    describe('getDataPillSampleValue', () => {
        const sampleOutput = {
            outputFiles: {'charts/summary.txt': {name: 'summary.txt'}, 'report.csv': {name: 'report.csv'}},
            tags: ['vip', 'new'],
            users: [{name: 'Ann'}],
        };

        it('reads a key that contains dots and slashes', () => {
            expect(getDataPillSampleValue(sampleOutput, ['outputFiles', 'charts/summary.txt', 'name'])).toBe(
                'summary.txt'
            );
            expect(getDataPillSampleValue(sampleOutput, ['outputFiles', 'report.csv'])).toEqual({name: 'report.csv'});
        });

        it('reads the first element for the array index placeholder', () => {
            expect(getDataPillSampleValue(sampleOutput, ['tags', '[index]'])).toBe('vip');
        });

        it('reads through an array to the first element when a key follows it', () => {
            expect(getDataPillSampleValue(sampleOutput, ['users', 'name'])).toBe('Ann');
        });

        it('returns undefined for a missing key', () => {
            expect(getDataPillSampleValue(sampleOutput, ['outputFiles', 'missing.csv', 'name'])).toBeUndefined();
        });
    });
});
