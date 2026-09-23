import {describeAttentionReason} from '@/ee/pages/embedded/automation-hub/utils/attentionReason';
import {describe, expect, it} from 'vitest';

describe('describeAttentionReason', () => {
    it('describes a missing connection', () => {
        expect(describeAttentionReason('MISSING_CONNECTION:slack')).toBe('Connect slack to keep this running');
    });

    it('describes a required input', () => {
        expect(describeAttentionReason('INPUT_REQUIRED:channel')).toBe('Fill in channel to keep this running');
    });

    it('falls back to a generic noun for a bare missing connection prefix', () => {
        expect(describeAttentionReason('MISSING_CONNECTION:')).toBe(
            'Connect the missing connection to keep this running'
        );
    });

    it('falls back to a generic noun for a bare required input prefix', () => {
        expect(describeAttentionReason('INPUT_REQUIRED:')).toBe('Fill in the required input to keep this running');
    });

    it('describes a pending update', () => {
        expect(describeAttentionReason('UPDATE_PENDING')).toBe('Turn it on again to apply the latest update');
    });

    it('returns undefined when healthy', () => {
        expect(describeAttentionReason(undefined)).toBeUndefined();
    });
});
