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

    it.each(['BLOCKED', 'REDACTED', 'WITHHELD'] as const)('maps a %s call without a duration', (status) => {
        expect(
            toClusterElementNodeState({
                endDate: '2026-10-23T10:00:00.412Z',
                startDate: '2026-10-23T10:00:00Z',
                status,
            })
        ).toEqual({clusterElement: true, status});
    });

    it.each(['BLOCKED', 'WITHHELD'] as const)('keeps a %s state when a later call completes', (status) => {
        expect(
            toClusterElementNodeState(
                {endDate: '2026-10-23T10:00:00.412Z', startDate: '2026-10-23T10:00:00Z', status: 'COMPLETED'},
                {clusterElement: true, status}
            )
        ).toEqual({clusterElement: true, status});
    });

    it('keeps a blocked state when a later call starts', () => {
        expect(toClusterElementNodeState({status: 'STARTED'}, {clusterElement: true, status: 'BLOCKED'})).toEqual({
            clusterElement: true,
            status: 'BLOCKED',
        });
    });

    it('keeps a blocked state when a later call is redacted', () => {
        expect(toClusterElementNodeState({status: 'REDACTED'}, {clusterElement: true, status: 'BLOCKED'})).toEqual({
            clusterElement: true,
            status: 'BLOCKED',
        });
    });

    it('keeps a failed state when a later call is blocked', () => {
        expect(
            toClusterElementNodeState({status: 'BLOCKED'}, {clusterElement: true, error: 'boom', status: 'FAILED'})
        ).toEqual({clusterElement: true, error: 'boom', status: 'FAILED'});
    });

    it('lets a failed call replace a blocked state', () => {
        expect(
            toClusterElementNodeState({error: 'boom', status: 'FAILED'}, {clusterElement: true, status: 'BLOCKED'})
        ).toMatchObject({error: 'boom', status: 'FAILED'});
    });
});
