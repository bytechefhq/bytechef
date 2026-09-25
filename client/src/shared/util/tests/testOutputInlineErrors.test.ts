import {describe, expect, it} from 'vitest';

import {isInlineTestOutputGraphQlError, isInlineTestOutputUrl} from '../testOutputInlineErrors';

describe('isInlineTestOutputUrl', () => {
    it('matches the node test-output resource with or without a query string', () => {
        expect(
            isInlineTestOutputUrl(
                'http://localhost/api/platform/internal/workflows/wf-1/workflow-nodes/firecrawl_1/test-outputs?environmentId=1'
            )
        ).toBe(true);
        expect(isInlineTestOutputUrl('/workflows/wf-1/workflow-nodes/firecrawl_1/test-outputs')).toBe(true);
    });

    it('does not match the exists check or the sample-output upload', () => {
        expect(isInlineTestOutputUrl('/workflows/wf-1/workflow-nodes/trigger_1/test-outputs/exists?x=1')).toBe(false);
        expect(isInlineTestOutputUrl('/workflows/wf-1/workflow-nodes/node_1/test-outputs/sample-output')).toBe(false);
    });
});

describe('isInlineTestOutputGraphQlError', () => {
    it('matches only errors raised by the cluster element test mutation', () => {
        expect(isInlineTestOutputGraphQlError({path: ['saveClusterElementTestOutput']})).toBe(true);
        expect(isInlineTestOutputGraphQlError({path: ['workflow']})).toBe(false);
        expect(isInlineTestOutputGraphQlError({})).toBe(false);
    });
});
