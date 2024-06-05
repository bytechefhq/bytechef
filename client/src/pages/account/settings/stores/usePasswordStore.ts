/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface PasswordI {
    updateSuccess: boolean;
    updateFailure: boolean;

    changePassword: (currentPassword: string, newPassword: string) => void;
    reset: () => void;
}

const fetchChangePassword = async (data: string): Promise<Response> => {
    return await fetch('/api/account/change-password', {
        body: data,
        headers: {
            'Content-Type': 'application/json',
        },
        method: 'POST',
    }).then((response) => response);
};

export const usePasswordStore = create<PasswordI>()(
    devtools(
        (set, get) => {
            return {
                updateSuccess: false,
                updateFailure: false,

                changePassword: async (currentPassword: string, newPassword: string) => {
                    get().reset();

                    const response = await fetchChangePassword(JSON.stringify({currentPassword, newPassword}));

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

                reset: () => {
                    set((state) => ({
                        ...state,
                        updateSuccess: false,
                        updateFailure: false,
                    }));
                },
            };
        },
        {
            name: 'password',
        }
    )
);
