import {getCookie} from '@/shared/util/cookie-utils';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface ActivateI {
    activationSuccess: boolean;
    activationFailure: boolean;
    loading: boolean;

    activate: (key: string | null) => void;
}

const fetchActivate = async (key: string | null) => {
    return await fetch(`/api/activate?key=${key}`, {
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
        },
        method: 'GET',
    }).then((response) => response);
};

export const useActivateStore = create<ActivateI>()(
    devtools(
        (set, get) => ({
            activationSuccess: false,
            activationFailure: false,
            loading: false,

            activate: async (key: string | null) => {
                if (get().loading) {
                    return;
                }

                set((state) => ({
                    ...state,
                    activationSuccess: false,
                    activationFailure: false,
                    loading: true,
                }));

                const response = await fetchActivate(key);

                if (response.status === 200) {
                    set(() => ({
                        activationSuccess: true,
                        loading: false,
                    }));
                } else {
                    set(() => ({
                        activationFailure: true,
                        loading: false,
                    }));
                }
            },
        }),
        {
            name: 'activate',
        }
    )
);
