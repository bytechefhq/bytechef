import {describe, expect, it} from 'vitest';

import toClusterElementNodeState from './toClusterElementNodeState';

describe('toClusterElementNodeState', () => {
    it('maps STARTED to RUNNING', () => {
        expect(toClusterElementNodeState({startDate: '2026-10-23T10:00:00Z', status: 'STARTED'})).toEqual({
            clusterElement: true,
            status: 'RUNNING',
        });
    });

    it('maps COMPLETED with its duration', () => {
        expect(
            toClusterElementNodeState({
                endDate: '2026-10-23T10:00:00.412Z',
                startDate: '2026-10-23T10:00:00Z',
                status: 'COMPLETED',
            })
        ).toEqual({clusterElement: true, durationMillis: 412, status: 'COMPLETED'});
    });

    it('maps FAILED with its error', () => {
        expect(
            toClusterElementNodeState({
                endDate: new Date('2026-10-23T10:00:00.031Z'),
                error: 'boom',
                startDate: new Date('2026-10-23T10:00:00Z'),
                status: 'FAILED',
            })
        ).toEqual({clusterElement: true, durationMillis: 31, error: 'boom', status: 'FAILED'});
    });

    it('keeps a FAILED node failed when a later call completes', () => {
        expect(
            toClusterElementNodeState(
                {endDate: '2026-10-23T10:00:05Z', startDate: '2026-10-23T10:00:04Z', status: 'COMPLETED'},
                {durationMillis: 31, error: 'boom', status: 'FAILED'}
            )
        ).toEqual({clusterElement: true, durationMillis: 31, error: 'boom', status: 'FAILED'});
    });
});
