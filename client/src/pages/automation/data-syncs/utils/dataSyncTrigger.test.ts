import {DataSyncTriggerType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {describeTrigger} from './dataSyncTrigger';

describe('describeTrigger', () => {
    it('reads Manual for a manual sync', () => {
        expect(describeTrigger(DataSyncTriggerType.Manual, null)).toBe('Manual');
    });

    it('describes a cadence picked in the UI', () => {
        expect(describeTrigger(DataSyncTriggerType.Schedule, {frequencyKind: 'DAILY', timeOfDay: '09:00'})).toBe(
            'Daily at 09:00'
        );
    });

    it('falls back to the raw expression', () => {
        expect(describeTrigger(DataSyncTriggerType.Schedule, {expression: '0 9 * * ?', frequencyKind: 'DAILY'})).toBe(
            '0 9 * * ?'
        );
    });
});
