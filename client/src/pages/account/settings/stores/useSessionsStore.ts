/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface SessionI {
    ipAddress: string;
    series: string;
    tokenDate: string;
    userAgent: string;
}

export interface SessionsI {
    loading: boolean;
    sessions: SessionI[];
    updateSuccess: boolean;
    updateFailure: boolean;

    getSessions: () => void;
    invalidateSession: (series: string) => void;
    reset: () => void;
}

const apiUrl = '/api/account/sessions';

const fetchGetSessions = async (): Promise<Response> => {
    return await fetch(apiUrl, {
        method: 'GET',
    }).then((response) => response);
};

const fetchInvalidateSession = async (series: string): Promise<Response> => {
    return await fetch(`${apiUrl}/${series}`, {
        method: 'DELETE',
    }).then((response) => response);
};

export const useSessionsStore = create<SessionsI>()(
    devtools(
        (set, get) => {
            return {
                loading: false,
                sessions: [],
                updateSuccess: false,
                updateFailure: false,

                getSessions: async () => {
                    set((state) => ({
                        ...state,
                        loading: true,
                    }));

                    const response = await fetchGetSessions();

                    const sessions = await response.json();

                    set((state) => ({
                        ...state,
                        loading: false,
                        sessions,
                    }));
                },

                invalidateSession: async (series: string) => {
                    const response = await fetchInvalidateSession(series);

                    if (response.status === 200) {
                        set((state) => ({
                            ...state,
                            updateSuccess: true,
                        }));

                        const {getSessions} = get();

                        getSessions();
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
                        loading: false,
                        updateSuccess: false,
                        updateFailure: false,
                    }));
                },
            };
        },
        {
            name: 'sessions',
        }
    )
);
