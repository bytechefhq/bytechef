import {UserI} from '@/shared/models/user.model';
import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface AuthenticationI {
    account: UserI | undefined;
    authenticated: boolean;
    loading: boolean;
    loginError: boolean;
    sessionHasBeenFetched: boolean;
    showLogin: boolean;

    clearAuthentication: () => void;
    getAccount: () => void;
    login: (email: string, password: string, rememberMe: boolean) => void;
    logout: () => void;
}

const initialState = {
    account: undefined,
    authenticated: false,
    loading: false,
    loginError: false,
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

export const useAuthenticationStore = create<AuthenticationI>()(
    devtools(
        (set, get) => ({
            account: undefined,
            authenticated: false,
            loading: false,
            loginError: false,
            sessionHasBeenFetched: false,
            showLogin: false,

            clearAuthentication: () => {
                set(() => ({
                    loading: false,
                    showModalLogin: true,
                }));
            },

            getAccount: async () => {
                if (get().loading) {
                    return;
                }

                set((state) => ({
                    ...state,
                    loading: true,
                }));

                const response = await fetchGetAccount();

                if (response.status === 200) {
                    const account = (await response.json()) as UserI;

                    set((state) => ({
                        ...state,
                        account,
                        authenticated: account.activated,
                        loading: false,
                        sessionHasBeenFetched: true,
                    }));
                } else {
                    set((state) => ({
                        ...state,
                        loading: false,
                        isAuthenticated: false,
                        sessionHasBeenFetched: true,
                        showLogin: true,
                    }));
                }
            },

            login: async (email: string, password: string, rememberMe: boolean) => {
                const data = `username=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}&remember-me=${rememberMe}&submit=Login`;

                const response = await fetchAuthenticate(data);

                if (response.status === 200) {
                    set((state) => ({
                        ...state,
                        loginError: false,
                        loginSuccess: true,
                        showLogin: false,
                    }));

                    const {getAccount} = get();

                    getAccount();
                } else {
                    set(() => ({
                        ...initialState,
                        loginError: true,
                        showLogin: true,
                    }));
                }
            },

            logout: async () => {
                const response = await fetchLogout();

                if (response.status === 200) {
                    set(() => ({
                        ...initialState,
                        showLogin: true,
                    }));
                }

                // const {getAccount} = get();

                // fetch new csrf token
                // getAccount();
            },
        }),
        {
            name: 'authentication',
        }
    )
);
