/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type EditionType = 'ce' | 'ee';

export interface ApplicationInfoI {
    application: {
        edition: EditionType;
    } | null;
    featureFlags: Record<string, boolean> | null;
    signUp: {
        activationRequired: boolean;
        enabled: boolean;
    };

    getApplicationInfo: () => void;
}

const fetchGetApplicationInfo = async (): Promise<Response> => {
    return await fetch('/actuator/info', {
        method: 'GET',
    }).then((response) => response);
};

export const useApplicationInfoStore = create<ApplicationInfoI>()(
    devtools(
        (set) => {
            return {
                application: null,
                featureFlags: {},
                signUp: {
                    activationRequired: false,
                    enabled: true,
                },

                getApplicationInfo: async () => {
                    const response = await fetchGetApplicationInfo();

                    if (response.status === 200) {
                        const json = await response.json();

                        set((state) => ({
                            ...state,
                            application: json.application,
                            featureFlags: json.featureFlags || {},
                            signUp: json.signUp,
                        }));
                    }
                },
            };
        },
        {
            name: 'application-info',
        }
    )
);
