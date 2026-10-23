import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useWorkspaceScopeEnvironmentIds} from '../useWorkspaceScopeEnvironmentIds';

const hoisted = vi.hoisted(() => ({
    authorities: ['ROLE_USER'] as string[],
    ceEdition: false,
    eeEdition: true,
    scopesByEnvironment: {} as Record<string, string[] | undefined>,
}));

vi.mock('@/shared/edition/useCeEdition', () => ({
    default: () => hoisted.ceEdition,
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

describe('useWorkspaceScopeEnvironmentIds', () => {
    beforeEach(() => {
        hoisted.authorities = ['ROLE_USER'];
        hoisted.ceEdition = false;
        hoisted.eeEdition = true;
        hoisted.scopesByEnvironment = {
            DEVELOPMENT: ['DEPLOYMENT_CREATE', 'DEPLOYMENT_VIEW'],
            PRODUCTION: ['DEPLOYMENT_VIEW'],
            STAGING: ['DEPLOYMENT_CREATE'],
        };
    });

    it('returns the environments in which the member holds the scope', () => {
        const {result} = renderHook(() => useWorkspaceScopeEnvironmentIds(7, 'DEPLOYMENT_CREATE'));

        expect(result.current).toEqual([0, 1]);
    });

    it('leaves out an environment whose scopes have not loaded', () => {
        hoisted.scopesByEnvironment.STAGING = undefined;

        const {result} = renderHook(() => useWorkspaceScopeEnvironmentIds(7, 'DEPLOYMENT_CREATE'));

        expect(result.current).toEqual([0]);
    });

    it('returns no environment without a workspace', () => {
        const {result} = renderHook(() => useWorkspaceScopeEnvironmentIds(undefined, 'DEPLOYMENT_CREATE'));

        expect(result.current).toEqual([]);
    });

    it('returns no environment while the edition is unresolved', () => {
        hoisted.eeEdition = false;

        const {result} = renderHook(() => useWorkspaceScopeEnvironmentIds(7, 'DEPLOYMENT_CREATE'));

        expect(result.current).toEqual([]);
    });

    it('allows every environment for a tenant admin', () => {
        hoisted.authorities = ['ROLE_ADMIN'];

        const {result} = renderHook(() => useWorkspaceScopeEnvironmentIds(7, 'DEPLOYMENT_CREATE'));

        expect(result.current).toBeUndefined();
    });

    it('allows every environment on Community', () => {
        hoisted.ceEdition = true;
        hoisted.eeEdition = false;

        const {result} = renderHook(() => useWorkspaceScopeEnvironmentIds(7, 'DEPLOYMENT_CREATE'));

        expect(result.current).toBeUndefined();
    });
});
