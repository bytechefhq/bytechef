import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// vi.hoisted ensures these setter references exist before the vi.mock factories evaluate — Vitest
// hoists mocks above imports, so without hoisted state the factories would close over uninitialized
// bindings. Each mock mutates its own slice of state so tests can drive each store independently.
const mocks = vi.hoisted(() => ({
    _app: {application: null as {edition: 'CE' | 'EE'} | null},
    _auth: {account: null as {authorities?: string[]} | null},
    _workspace: {currentWorkspaceId: undefined as number | undefined},
    setAuthorities(authorities: string[] | undefined) {
        mocks._auth.account = authorities ? {authorities} : null;
    },
    setCurrentWorkspaceId(id: number | undefined) {
        mocks._workspace.currentWorkspaceId = id;
    },
    setEdition(edition: 'CE' | 'EE' | null) {
        mocks._app.application = edition === null ? null : {edition};
    },
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: <T>(selector: (state: typeof mocks._workspace) => T) => selector(mocks._workspace),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    EditionType: {CE: 'CE', EE: 'EE'},
    useApplicationInfoStore: <T>(selector: (state: typeof mocks._app) => T) => selector(mocks._app),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: <T>(selector: (state: typeof mocks._auth) => T) => selector(mocks._auth),
}));

import {useIsVisibilityEditionEnabled, useVisibilityFeatureEnabled} from '../useVisibilityFeatureEnabled';

describe('useVisibilityFeatureEnabled', () => {
    beforeEach(() => {
        mocks.setCurrentWorkspaceId(undefined);
        mocks.setEdition(null);
        mocks.setAuthorities(undefined);
    });

    it('returns enabled=false in CE — the safe default that hides EE-only UI from CE users', () => {
        mocks.setEdition('CE');
        mocks.setCurrentWorkspaceId(5);

        const {result} = renderHook(() => useVisibilityFeatureEnabled());

        expect(result.current.enabled).toBe(false);
    });

    it('returns enabled=false when application info has not loaded yet', () => {
        mocks.setCurrentWorkspaceId(5);

        const {result} = renderHook(() => useVisibilityFeatureEnabled());

        expect(result.current.enabled).toBe(false);
    });

    it('returns enabled=false on EE when no workspace is selected (e.g. platform pages)', () => {
        mocks.setEdition('EE');

        const {result} = renderHook(() => useVisibilityFeatureEnabled());

        expect(result.current.enabled).toBe(false);
    });

    it('returns enabled=true with concrete workspaceId when EE + workspace is set', () => {
        mocks.setEdition('EE');
        mocks.setCurrentWorkspaceId(7);

        const {result} = renderHook(() => useVisibilityFeatureEnabled());

        expect(result.current.enabled).toBe(true);

        if (result.current.enabled) {
            // discriminated-union narrowing: workspaceId is guaranteed number when enabled
            expect(result.current.workspaceId).toBe(7);
        }
    });

    it('exposes isAdmin=true when the auth store reports ROLE_ADMIN', () => {
        mocks.setEdition('EE');
        mocks.setCurrentWorkspaceId(7);
        mocks.setAuthorities(['ROLE_ADMIN']);

        const {result} = renderHook(() => useVisibilityFeatureEnabled());

        expect(result.current.isAdmin).toBe(true);
    });

    it('returns isAdmin=false when the user lacks ROLE_ADMIN', () => {
        mocks.setEdition('EE');
        mocks.setCurrentWorkspaceId(7);
        mocks.setAuthorities(['ROLE_USER']);

        const {result} = renderHook(() => useVisibilityFeatureEnabled());

        expect(result.current.isAdmin).toBe(false);
    });
});

describe('useIsVisibilityEditionEnabled', () => {
    beforeEach(() => {
        mocks.setEdition(null);
    });

    it('returns false in CE', () => {
        mocks.setEdition('CE');

        const {result} = renderHook(() => useIsVisibilityEditionEnabled());

        expect(result.current).toBe(false);
    });

    it('returns true in EE', () => {
        mocks.setEdition('EE');

        const {result} = renderHook(() => useIsVisibilityEditionEnabled());

        expect(result.current).toBe(true);
    });

    it('returns false when application info has not loaded yet', () => {
        const {result} = renderHook(() => useIsVisibilityEditionEnabled());

        expect(result.current).toBe(false);
    });
});
