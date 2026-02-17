import {UserI} from '@/shared/models/user.model';
import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';

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
    logout: () => void;
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
        (set, get) => ({
            account: undefined,
            authenticated: false,
            loading: false,
            loginError: false,
            mfaRequired: false,
            sessionHasBeenFetched: false,
            showLogin: false,

            clearAuthentication: () => {
                set((state) => ({
                    ...state,
                    loading: false,
                    mfaRequired: false,
                    showLogin: true,
                    authenticated: false,
                }));
            },

            getAccount: async (): Promise<UserI | undefined> => {
                if (get().loading) {
                    return;
                }

                set((state) => ({
                    ...state,
                    loading: true,
                }));

                return fetchGetAccount().then((response) => {
                    if (response.status === 200) {
                        return response.json().then((account) => {
                            set((state) => ({
                                ...state,
                                account,
                                authenticated: account.activated,
                                loading: false,
                                sessionHasBeenFetched: true,
                            }));

                            return account;
                        });
                    } else {
                        set((state) => ({
                            ...state,
                            loading: false,
                            isAuthenticated: false,
                            sessionHasBeenFetched: true,
                            showLogin: true,
                        }));
                    }
                });
            },

            login: async (email: string, password: string, rememberMe: boolean): Promise<UserI | undefined> => {
                const data = `username=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}&remember-me=${rememberMe}&submit=Login`;

                return fetchAuthenticate(data).then((response) => {
                    if (response.status === 200) {
                        set((state) => ({
                            ...state,
                            loginError: false,
                            loginSuccess: true,
                            showLogin: false,
                        }));

                        const {getAccount} = get();

                        return getAccount();
                    } else if (response.status === 202) {
                        set((state) => ({
                            ...state,
                            loginError: false,
                            mfaRequired: true,
                            showLogin: false,
                        }));
                    } else {
                        set(() => ({
                            ...initialState,
                            loginError: true,
                            showLogin: true,
                        }));
                    }
                });
            },

            logout: async () => {
                const response = await fetchLogout();

                if (response.status === 200) {
                    set(() => ({
                        ...initialState,
                        showLogin: true,
                    }));
                }

                const {getAccount} = get();

                // fetch new csrf token
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
                        set((state) => ({
                            ...state,
                            mfaRequired: false,
                        }));

                        const {getAccount} = get();

                        return getAccount();
                    } else {
                        set((state) => ({
                            ...state,
                            loginError: true,
                        }));

                        return undefined;
                    }
                } catch {
                    set((state) => ({
                        ...state,
                        loginError: true,
                    }));

                    return undefined;
                }
            },

            reset: () => {
                set(() => ({
                    ...initialState,
                }));
            },
        }),
        {
            name: 'authentication',
        }
    )
);

export function useAuthenticationStore<U>(selector: (state: ExtractState<typeof authenticationStore>) => U): U {
    return useStore(authenticationStore, selector);
}
