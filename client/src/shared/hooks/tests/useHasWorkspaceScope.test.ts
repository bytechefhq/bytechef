import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({ceEdition: false, tenantAdmin: false}));

vi.mock('@/shared/edition/useCeEdition', () => ({default: () => hoisted.ceEdition}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({useIsTenantAdmin: () => hoisted.tenantAdmin}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

describe('useHasWorkspaceScope', () => {
    beforeEach(() => {
        hoisted.ceEdition = false;
        hoisted.tenantAdmin = false;

        permissionStore.setState({workspaceScopeStates: {}});
    });

    it('grants every scope on Community', () => {
        hoisted.ceEdition = true;

        const {result} = renderHook(() => useHasWorkspaceScope(7, 'API_KEY_CREATE'));

        expect(result.current).toBe(true);
    });

    it('grants every scope to a tenant admin', () => {
        hoisted.tenantAdmin = true;

        const {result} = renderHook(() => useHasWorkspaceScope(7, 'API_KEY_CREATE'));

        expect(result.current).toBe(true);
    });

    it('grants a scope the member holds in the selected environment', () => {
        permissionStore.setState({
            workspaceScopeStates: {7: {2: {scopes: ['API_KEY_VIEW', 'API_KEY_CREATE'], status: 'loaded'}}},
        });

        const {result} = renderHook(() => useHasWorkspaceScope(7, 'API_KEY_CREATE'));

        expect(result.current).toBe(true);
    });

    it('denies a scope the member does not hold', () => {
        permissionStore.setState({workspaceScopeStates: {7: {2: {scopes: [], status: 'loaded'}}}});

        const {result} = renderHook(() => useHasWorkspaceScope(7, 'API_KEY_CREATE'));

        expect(result.current).toBe(false);
    });

    it('denies while the scopes are loading', () => {
        permissionStore.setState({workspaceScopeStates: {7: {2: {status: 'loading'}}}});

        const {result} = renderHook(() => useHasWorkspaceScope(7, 'API_KEY_VIEW'));

        expect(result.current).toBe(false);
    });
});
