import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface RegisterI {
    registerAlreadyUsedEmail: string;
    registerErrorMessage: string;
    registerSuccess: boolean;

    register: (email: string, password: string) => Promise<void>;
    reset: () => void;
}

const EMAIL_ALREADY_USED_ERROR_KEY = 101;

const fetchRegister = async (data: string): Promise<Response> => {
    return await fetch('/api/register', {
        body: data,
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'POST',
    }).then((response) => response);
};

export const useRegisterStore = create<RegisterI>()(
    devtools(
        (set) => ({
            registerAlreadyUsedEmail: '',
            registerSuccess: false,
            registerErrorMessage: '',

            register: async (email: string, password: string): Promise<void> => {
                return fetchRegister(JSON.stringify({email, langKey: 'en', login: email, password})).then(
                    (response) => {
                        if (response.status === 201) {
                            set(() => ({
                                registerSuccess: true,
                            }));
                        } else {
                            response.json().then((data) => {
                                if (data.entityClass === 'User' && data.errorKey === EMAIL_ALREADY_USED_ERROR_KEY) {
                                    set(() => ({
                                        registerAlreadyUsedEmail: email,
                                    }));
                                } else {
                                    set(() => ({
                                        registerErrorMessage: data.detail,
                                    }));
                                }
                            });
                        }
                    }
                );
            },

            reset: () => {
                set(() => ({
                    registerAlreadyUsedEmail: '',
                    registerErrorMessage: '',
                    registerSuccess: false,
                }));
            },
        }),
        {
            name: 'register',
        }
    )
);
