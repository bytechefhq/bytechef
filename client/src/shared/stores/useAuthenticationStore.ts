import {UserI} from '@/shared/models/user.model';
import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';

import {permissionStore} from './usePermissionStore';

import type {ExtractState} from 'zustand/vanilla';

export interface AuthenticationI {
    account: UserI | undefined;
    authenticated: boolean;
    loading: boolean;
    loginError: boolean;
    mfaRequired: boolean;
    sessionHasBeenFetched: boolean;
    showLogin: boolean;
    clearAuthentication: () => void;
    getAccount: () => Promise<UserI | undefined>;
    login: (email: string, password: string, rememberMe: boolean) => Promise<UserI | undefined>;
    logout: () => Promise<void>;
    reset: () => void;
    verifyMfa: (code: string) => Promise<UserI | undefined>;
}

const initialState = {
    account: undefined,
    authenticated: false,
    loading: false,
    loginError: false,
    mfaRequired: false,
    sessionHasBeenFetched: false,
    showLogin: false,
} satisfies Partial<AuthenticationI>;

let accountRequest: Promise<UserI | undefined> | undefined;

const invalidateAccountRequest = () => {
    accountRequest = undefined;
};

const fetchAuthenticate = async (data: string): Promise<Response> => {
    return await fetch('/api/authentication', {
        body: data,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    }).then((response) => response);
};

const fetchGetAccount = async (): Promise<Response> => {
    return await fetch('/api/account', {
        headers: {
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'GET',
    }).then((response) => response);
};

const fetchLogout = async (): Promise<Response> => {
    return await fetch('/api/logout', {
        headers: {
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    }).then((response) => response);
};

export const authenticationStore = createStore<AuthenticationI>()(
    devtools(
        (set, get) => {
            const setAuthenticationState = (partialState: Partial<AuthenticationI>) => set(partialState);

            return {
                ...initialState,

                clearAuthentication: () => {
                    invalidateAccountRequest();

                    setAuthenticationState({
                        loading: false,
                        mfaRequired: false,
                        showLogin: true,
                        authenticated: false,
                    });

                    permissionStore.getState().clearPermissions();
                },

                getAccount: (): Promise<UserI | undefined> => {
                    if (accountRequest) {
                        return accountRequest;
                    }

                    setAuthenticationState({
                        loading: true,
                    });

                    const request: Promise<UserI | undefined> = fetchGetAccount()
                        .then(async (response) => {
                            if (response.status === 200) {
                                const account: UserI = await response.json();

                                if (accountRequest === request) {
                                    setAuthenticationState({
                                        account,
                                        authenticated: account.activated === true,
                                        sessionHasBeenFetched: true,
                                    });
                                }

                                return account;
                            }

                            if (accountRequest === request) {
                                setAuthenticationState({
                                    authenticated: false,
                                    sessionHasBeenFetched: true,
                                    showLogin: true,
                                });
                            }

                            return undefined;
                        })
                        .finally(() => {
                            if (accountRequest === request) {
                                accountRequest = undefined;

                                setAuthenticationState({
                                    loading: false,
                                });
                            }
                        });

                    accountRequest = request;

                    return request;
                },

                login: async (email: string, password: string, rememberMe: boolean): Promise<UserI | undefined> => {
                    const data = `username=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}&remember-me=${rememberMe}&submit=Login`;

                    return fetchAuthenticate(data).then((response) => {
                        if (response.status === 200) {
                            setAuthenticationState({
                                loginError: false,
                                showLogin: false,
                            });

                            const {getAccount} = get();

                            return getAccount();
                        } else if (response.status === 202) {
                            setAuthenticationState({
                                loginError: false,
                                mfaRequired: true,
                                showLogin: false,
                            });
                        } else {
                            invalidateAccountRequest();

                            setAuthenticationState({
                                ...initialState,
                                loginError: true,
                                showLogin: true,
                            });

                            permissionStore.getState().clearPermissions();
                        }
                    });
                },

                logout: async () => {
                    const response = await fetchLogout();

                    if (response.status === 200) {
                        setAuthenticationState({
                            ...initialState,
                            showLogin: true,
                        });

                        permissionStore.getState().clearPermissions();
                    }

                    // fetch new csrf token
                    invalidateAccountRequest();

                    const {getAccount} = get();

                    getAccount();
                },

                verifyMfa: async (code: string): Promise<UserI | undefined> => {
                    try {
                        const response = await fetch('/api/mfa/verify', {
                            body: JSON.stringify({code}),
                            headers: {
                                'Content-Type': 'application/json',
                                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                            },
                            method: 'POST',
                        });

                        if (response.status === 200) {
                            setAuthenticationState({
                                mfaRequired: false,
                            });

                            const {getAccount} = get();

                            return getAccount();
                        } else {
                            setAuthenticationState({
                                loginError: true,
                            });

                            permissionStore.getState().clearPermissions();

                            return undefined;
                        }
                    } catch {
                        setAuthenticationState({
                            loginError: true,
                        });

                        permissionStore.getState().clearPermissions();

                        return undefined;
                    }
                },

                reset: () => {
                    invalidateAccountRequest();

                    setAuthenticationState({
                        ...initialState,
                    });

                    permissionStore.getState().clearPermissions();
                },
            };
        },
        {
            name: 'authentication',
        }
    )
);

export function useAuthenticationStore<U>(selector: (state: ExtractState<typeof authenticationStore>) => U): U {
    return useStore(authenticationStore, selector);
}
