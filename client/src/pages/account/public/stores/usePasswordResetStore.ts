import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface PasswordResetI {
    resetPasswordSuccess: boolean;
    resetPasswordFailure: boolean;

    reset: () => void;
    resetPasswordInit: (email: string) => void;
    resetPasswordFinish: (key: string | null, newPassword: string) => void;
}

const initialState = {
    resetPasswordSuccess: false,
    resetPasswordFailure: false,
};

const fetchResetPasswordInit = async (email: string): Promise<Response> => {
    return await fetch('/api/account/reset-password/init', {
        body: email,
        headers: {
            'Content-Type': 'text/plain',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    }).then((response) => response);
};

const fetchResetPasswordFinish = async (data: string): Promise<Response> => {
    return await fetch('/api/account/reset-password/finish', {
        body: data,
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    }).then((response) => response);
};

export const usePasswordResetStore = create<PasswordResetI>()(
    devtools(
        (set) => ({
            resetPasswordSuccess: false,
            resetPasswordFailure: false,

            reset: () => {
                set(() => ({
                    ...initialState,
                }));
            },

            resetPasswordInit: async (email: string) => {
                const response = await fetchResetPasswordInit(email);

                if (response.status === 200) {
                    set(() => ({
                        ...initialState,
                        resetPasswordSuccess: true,
                    }));
                } else {
                    set(() => ({
                        ...initialState,
                        resetPasswordFailure: true,
                    }));
                }
            },

            resetPasswordFinish: async (key: string | null, newPassword: string) => {
                const response = await fetchResetPasswordFinish(JSON.stringify({key, newPassword}));

                if (response.status === 200) {
                    set(() => ({
                        ...initialState,
                        resetPasswordSuccess: true,
                    }));
                }
            },
        }),
        {
            name: 'password-reset',
        }
    )
);
