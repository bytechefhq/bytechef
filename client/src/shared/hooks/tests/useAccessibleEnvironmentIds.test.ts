import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useAccessibleEnvironmentIds} from '../useAccessibleEnvironmentIds';

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
    useMyWorkspaceScopesQuery: (variables: {environment: string}, options: {enabled: boolean}) => {
        const scopes = hoisted.scopesByEnvironment[variables.environment];

        return {data: options.enabled && scopes !== undefined ? {myWorkspaceScopes: scopes} : undefined};
    },
}));

describe('useAccessibleEnvironmentIds', () => {
    beforeEach(() => {
        hoisted.authorities = ['ROLE_USER'];
        hoisted.eeEdition = true;
        hoisted.scopesByEnvironment = {
            DEVELOPMENT: ['WORKFLOW_VIEW'],
            PRODUCTION: [],
            STAGING: ['WORKFLOW_VIEW'],
        };
    });

    it('returns the environments in which the member holds any scope', () => {
        const {result} = renderHook(() => useAccessibleEnvironmentIds(7));

        expect(result.current).toEqual([0, 1]);
    });

    it('returns nothing until every environment has answered', () => {
        hoisted.scopesByEnvironment = {DEVELOPMENT: ['WORKFLOW_VIEW'], PRODUCTION: undefined, STAGING: []};

        const {result} = renderHook(() => useAccessibleEnvironmentIds(7));

        expect(result.current).toBeUndefined();
    });

    it('returns nothing for a tenant admin', () => {
        hoisted.authorities = ['ROLE_ADMIN'];

        const {result} = renderHook(() => useAccessibleEnvironmentIds(7));

        expect(result.current).toBeUndefined();
    });

    it('returns nothing outside the Enterprise edition', () => {
        hoisted.eeEdition = false;

        const {result} = renderHook(() => useAccessibleEnvironmentIds(7));

        expect(result.current).toBeUndefined();
    });

    it('returns nothing without a workspace', () => {
        const {result} = renderHook(() => useAccessibleEnvironmentIds(undefined));

        expect(result.current).toBeUndefined();
    });
});
