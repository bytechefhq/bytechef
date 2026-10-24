import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {elementKindKey, elementNodeName, findElement} from './dataSyncElements';

describe('dataSyncElements', () => {
    it('finds the element of a kind', () => {
        const source = {componentName: 'csvFile', id: '1', kind: DataSyncElementKind.Source};
        const dataSync = {elements: [source, {componentName: 'pg', id: '2', kind: DataSyncElementKind.Destination}]};

        expect(findElement(dataSync, DataSyncElementKind.Source)).toBe(source);
        expect(findElement(dataSync, DataSyncElementKind.Processor)).toBeUndefined();
    });

    it('maps a kind to its cluster element key', () => {
        expect(elementKindKey(DataSyncElementKind.Source)).toBe('source');
        expect(elementKindKey(DataSyncElementKind.Destination)).toBe('destination');
        expect(elementKindKey(DataSyncElementKind.Processor)).toBe('processor');
    });

    it('matches the server-generated workflow node names exactly', () => {
        expect(elementNodeName(DataSyncElementKind.Source)).toBe('source_1');
        expect(elementNodeName(DataSyncElementKind.Destination)).toBe('destination_1');
        expect(elementNodeName(DataSyncElementKind.Processor)).toBe('processor_1');
    });
});
