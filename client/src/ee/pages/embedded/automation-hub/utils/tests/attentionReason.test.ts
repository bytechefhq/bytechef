import {
    describeAttentionReason,
    isInvalidWorkflowAttentionReason,
} from '@/ee/pages/embedded/automation-hub/utils/attentionReason';
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

    it('describes a workflow with one error', () => {
        expect(describeAttentionReason('INVALID_WORKFLOW:1')).toBe('Fix the error in this workflow to turn it on');
    });

    it('describes a workflow with several errors', () => {
        expect(describeAttentionReason('INVALID_WORKFLOW:3')).toBe('Fix the 3 errors in this workflow to turn it on');
    });

    it('falls back to a generic noun for an invalid workflow without a usable count', () => {
        expect(describeAttentionReason('INVALID_WORKFLOW:')).toBe('Fix the errors in this workflow to turn it on');
    });

    it('describes a pending update', () => {
        expect(describeAttentionReason('UPDATE_PENDING')).toBe('Turn it on again to apply the latest update');
    });

    it('returns undefined when healthy', () => {
        expect(describeAttentionReason(undefined)).toBeUndefined();
    });
});

describe('isInvalidWorkflowAttentionReason', () => {
    it('recognizes an invalid workflow', () => {
        expect(isInvalidWorkflowAttentionReason('INVALID_WORKFLOW:2')).toBe(true);
    });

    it('rejects the other reasons and a healthy automation', () => {
        expect(isInvalidWorkflowAttentionReason('MISSING_CONNECTION:slack')).toBe(false);
        expect(isInvalidWorkflowAttentionReason(undefined)).toBe(false);
    });
});
