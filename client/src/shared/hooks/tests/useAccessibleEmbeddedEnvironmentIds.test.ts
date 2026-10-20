import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useAccessibleEmbeddedEnvironmentIds} from '../useAccessibleEmbeddedEnvironmentIds';

const hoisted = vi.hoisted(() => ({
    authorities: ['ROLE_USER'] as string[],
    eeEdition: true,
    scopesByEnvironment: {} as Record<string, string[] | undefined>,
}));

vi.mock('@/shared/edition/useEeEdition', () => ({
    default: () => hoisted.eeEdition,
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({account: {authorities: hoisted.authorities}, authenticated: true}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    EnvironmentEnum: {Development: 'DEVELOPMENT', Production: 'PRODUCTION', Staging: 'STAGING'},
    useMyEmbeddedScopesQuery: (variables: {environment: string}, options: {enabled: boolean}) => {
        const scopes = hoisted.scopesByEnvironment[variables.environment];

        return {data: options.enabled && scopes !== undefined ? {myEmbeddedScopes: scopes} : undefined};
    },
}));

describe('useAccessibleEmbeddedEnvironmentIds', () => {
    beforeEach(() => {
        hoisted.authorities = ['ROLE_USER'];
        hoisted.eeEdition = true;
        hoisted.scopesByEnvironment = {
            DEVELOPMENT: ['WORKFLOW_VIEW'],
            PRODUCTION: [],
            STAGING: ['WORKFLOW_VIEW'],
        };
    });

    it('testReturnsTheEnvironmentsInWhichTheMemberHoldsAnyScope', () => {
        const {result} = renderHook(() => useAccessibleEmbeddedEnvironmentIds());

        expect(result.current).toEqual([0, 1]);
    });

    it('testReturnsNothingUntilEveryEnvironmentHasAnswered', () => {
        hoisted.scopesByEnvironment = {DEVELOPMENT: ['WORKFLOW_VIEW'], PRODUCTION: undefined, STAGING: []};

        const {result} = renderHook(() => useAccessibleEmbeddedEnvironmentIds());

        expect(result.current).toBeUndefined();
    });

    it('testReturnsNothingForATenantAdmin', () => {
        hoisted.authorities = ['ROLE_ADMIN'];

        const {result} = renderHook(() => useAccessibleEmbeddedEnvironmentIds());

        expect(result.current).toBeUndefined();
    });

    it('testReturnsNothingOutsideTheEnterpriseEdition', () => {
        hoisted.eeEdition = false;

        const {result} = renderHook(() => useAccessibleEmbeddedEnvironmentIds());

        expect(result.current).toBeUndefined();
    });
});
