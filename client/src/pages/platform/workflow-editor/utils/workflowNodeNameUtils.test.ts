import {describe, expect, it} from 'vitest';

import {getNestedBottomGhostId, getWorkflowNodeComponentName, toWorkflowNodeNamePrefix} from './workflowNodeNameUtils';

describe('toWorkflowNodeNamePrefix', () => {
    it('camelCases a hyphenated dispatcher name so the node name is a valid expression identifier', () => {
        expect(toWorkflowNodeNamePrefix('fork-join')).toBe('forkJoin');
        expect(toWorkflowNodeNamePrefix('on-error')).toBe('onError');
    });

    it('leaves an already camelCased name untouched', () => {
        expect(toWorkflowNodeNamePrefix('httpClient')).toBe('httpClient');
        expect(toWorkflowNodeNamePrefix('condition')).toBe('condition');
    });
});

describe('getWorkflowNodeComponentName', () => {
    it('maps a camelCased dispatcher node name back to its component name', () => {
        expect(getWorkflowNodeComponentName('forkJoin_1')).toBe('fork-join');
        expect(getWorkflowNodeComponentName('onError_12')).toBe('on-error');
    });

    it('still reads a hyphenated node name saved before the rename', () => {
        expect(getWorkflowNodeComponentName('fork-join_1')).toBe('fork-join');
        expect(getWorkflowNodeComponentName('on-error_2')).toBe('on-error');
    });

    it('returns the name prefix of any other node', () => {
        expect(getWorkflowNodeComponentName('condition_3')).toBe('condition');
        expect(getWorkflowNodeComponentName('httpClient_1')).toBe('httpClient');
    });
});

describe('getNestedBottomGhostId', () => {
    it('uses the camelCased ghost segment for both new and hyphenated dispatcher node names', () => {
        expect(getNestedBottomGhostId('forkJoin_1')).toBe('forkJoin_1-forkJoin-bottom-ghost');
        expect(getNestedBottomGhostId('fork-join_1')).toBe('fork-join_1-forkJoin-bottom-ghost');
        expect(getNestedBottomGhostId('onError_2')).toBe('onError_2-onError-bottom-ghost');
        expect(getNestedBottomGhostId('on-error_2')).toBe('on-error_2-onError-bottom-ghost');
    });

    it('uses the component name verbatim for the other dispatchers', () => {
        expect(getNestedBottomGhostId('condition_3')).toBe('condition_3-condition-bottom-ghost');
    });
});
