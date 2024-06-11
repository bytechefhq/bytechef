import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface RegisterI {
    registerErrorMessage: string;
    registerSuccess: boolean;

    register: (email: string, password: string) => void;
}

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
            registerSuccess: false,
            registerErrorMessage: '',

            register: async (email: string, password: string) => {
                const response = await fetchRegister(JSON.stringify({email, langKey: 'en', login: email, password}));

                if (response.status === 201) {
                    set(() => ({
                        registerSuccess: true,
                    }));
                } else {
                    const detail = (await response.json()).detail;

                    set(() => ({
                        registerErrorMessage: detail,
                    }));
                }
            },
        }),
        {
            name: 'register',
        }
    )
);
