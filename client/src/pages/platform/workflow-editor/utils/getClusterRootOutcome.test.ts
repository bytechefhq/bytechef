import {ClusterElementsType} from '@/shared/types';
import {describe, expect, it} from 'vitest';

import getClusterRootOutcome from './getClusterRootOutcome';

const CLUSTER_ELEMENTS = {
    model: {name: 'openAi_1', type: 'openAi/v1/model'},
    tools: [
        {name: 'httpClient_1', type: 'httpClient/v1/get'},
        {
            clusterElements: {tools: [{name: 'check_1', type: 'guardrails/v1/checkForViolations'}]},
            name: 'slack_1',
            type: 'slack/v1/sendMessage',
        },
    ],
} as unknown as ClusterElementsType;

describe('getClusterRootOutcome', () => {
    it('returns BLOCKED when a nested check is blocked', () => {
        expect(getClusterRootOutcome(CLUSTER_ELEMENTS, {check_1: {status: 'BLOCKED'}})).toBe('BLOCKED');
    });

    it('returns WITHHELD when the sanitize frame is withheld', () => {
        expect(getClusterRootOutcome(CLUSTER_ELEMENTS, {httpClient_1: {status: 'WITHHELD'}})).toBe('WITHHELD');
    });

    it('prefers BLOCKED over WITHHELD', () => {
        expect(
            getClusterRootOutcome(CLUSTER_ELEMENTS, {
                check_1: {status: 'BLOCKED'},
                httpClient_1: {status: 'WITHHELD'},
            })
        ).toBe('BLOCKED');
    });

    it('ignores failed and redacted elements', () => {
        expect(
            getClusterRootOutcome(CLUSTER_ELEMENTS, {
                httpClient_1: {status: 'FAILED'},
                openAi_1: {status: 'REDACTED'},
            })
        ).toBeUndefined();
    });

    it('returns undefined with no states', () => {
        expect(getClusterRootOutcome(CLUSTER_ELEMENTS, {})).toBeUndefined();
        expect(getClusterRootOutcome(undefined, {})).toBeUndefined();
    });
});
