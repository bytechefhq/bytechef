/* eslint-disable sort-keys */

import {UserI} from '@/shared/models/user.model';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface AccountI {
    updateSuccess: boolean;
    updateFailure: boolean;

    reset: () => void;
    updateAccount: (account: UserI) => void;
}

const fetchUpdateAccount = async (data: string): Promise<Response> => {
    return await fetch('/api/account', {
        body: data,
        headers: {
            'Content-Type': 'application/json',
        },
        method: 'POST',
    }).then((response) => response);
};

export const useAccountStore = create<AccountI>()(
    devtools(
        (set, get) => {
            return {
                updateSuccess: false,
                updateFailure: false,

                reset: () => {
                    set((state) => ({
                        ...state,
                        updateSuccess: false,
                        updateFailure: false,
                    }));
                },

                updateAccount: async (account: UserI) => {
                    get().reset();

                    const response = await fetchUpdateAccount(JSON.stringify(account));

                    if (response.status === 200) {
                        set((state) => ({
                            ...state,
                            updateSuccess: true,
                        }));
                    } else {
                        set((state) => ({
                            ...state,
                            updateFailure: true,
                        }));
                    }
                },
            };
        },
        {
            name: 'account',
        }
    )
);
