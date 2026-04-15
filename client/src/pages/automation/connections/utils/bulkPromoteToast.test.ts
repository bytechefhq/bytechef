import {BulkPromoteResult} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {buildBulkPromoteToast} from './bulkPromoteToast';

const makeResult = (overrides: Partial<BulkPromoteResult> = {}): BulkPromoteResult => ({
    attempted: 0,
    failed: 0,
    failures: [],
    promoted: 0,
    skipped: 0,
    ...overrides,
});

describe('buildBulkPromoteToast', () => {
    it('returns an info toast with no skipped suffix when no rows are skipped', () => {
        const {message, type} = buildBulkPromoteToast(makeResult({promoted: 3}));

        expect(type).toBe('info');
        expect(message).toBe('Promoted 3 private connection(s) to workspace.');
    });

    it('surfaces the skipped count separately from failed so a benign race does not look like an error', () => {
        const {message, type} = buildBulkPromoteToast(makeResult({promoted: 10, skipped: 2}));

        expect(type).toBe('info');
        expect(message).toBe('Promoted 10 private connection(s) to workspace, 2 skipped (already at target).');
    });

    it('emits an error toast including skipped and first three failures when failed > 0', () => {
        const {message, type} = buildBulkPromoteToast(
            makeResult({
                failed: 2,
                failures: [
                    {connectionId: '11', errorCode: 'UNEXPECTED', message: 'network'},
                    {connectionId: '22', errorCode: '102', message: 'conflict'},
                ],
                promoted: 5,
                skipped: 1,
            })
        );

        expect(type).toBe('error');
        expect(message).toBe('Promoted 5, 1 skipped (already at target), failed 2: #11 (network), #22 (conflict)');
    });

    it('truncates the failures list past the first three and appends an overflow count', () => {
        const {message} = buildBulkPromoteToast(
            makeResult({
                failed: 5,
                failures: [
                    {connectionId: '1', errorCode: 'UNEXPECTED', message: 'a'},
                    {connectionId: '2', errorCode: 'UNEXPECTED', message: 'b'},
                    {connectionId: '3', errorCode: 'UNEXPECTED', message: 'c'},
                    {connectionId: '4', errorCode: 'UNEXPECTED', message: 'd'},
                    {connectionId: '5', errorCode: 'UNEXPECTED', message: 'e'},
                ],
                promoted: 0,
            })
        );

        expect(message).toContain('#1 (a), #2 (b), #3 (c)');
        expect(message).toContain('(+2 more)');
        expect(message).not.toContain('#4');
    });
});
