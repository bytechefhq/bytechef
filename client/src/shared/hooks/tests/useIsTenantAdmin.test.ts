import {AUTHORITIES} from '@/shared/constants';
import {UserI} from '@/shared/models/user.model';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useIsTenantAdmin} from '../useIsTenantAdmin';

const createAccount = (authorities?: string[]): UserI => ({activated: true, authorities, login: 'user'});

describe('useIsTenantAdmin', () => {
    beforeEach(() => {
        authenticationStore.getState().reset();
    });

    it('returns true for an authenticated account with the admin authority', () => {
        authenticationStore.setState({account: createAccount([AUTHORITIES.ADMIN]), authenticated: true});

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(true);
    });

    it('returns true when the admin authority is one of several', () => {
        authenticationStore.setState({
            account: createAccount([AUTHORITIES.USER, AUTHORITIES.ADMIN]),
            authenticated: true,
        });

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(true);
    });

    it('returns false for an authenticated account without the admin authority', () => {
        authenticationStore.setState({account: createAccount([AUTHORITIES.USER]), authenticated: true});

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(false);
    });

    it('returns false when the account has no authorities', () => {
        authenticationStore.setState({account: createAccount(), authenticated: true});

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(false);
    });

    it('returns false when there is no account', () => {
        authenticationStore.setState({account: undefined, authenticated: true});

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(false);
    });

    it('returns false for an admin account whose session is no longer authenticated', () => {
        authenticationStore.setState({account: createAccount([AUTHORITIES.ADMIN]), authenticated: false});

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(false);
    });

    it('updates when the session is cleared', () => {
        authenticationStore.setState({account: createAccount([AUTHORITIES.ADMIN]), authenticated: true});

        const {result} = renderHook(() => useIsTenantAdmin());

        expect(result.current).toBe(true);

        act(() => {
            authenticationStore.getState().clearAuthentication();
        });

        expect(result.current).toBe(false);
    });
});
