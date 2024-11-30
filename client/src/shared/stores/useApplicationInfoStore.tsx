/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type EditionType = 'ce' | 'ee';

export interface ApplicationInfoI {
    ai: {
        copilot: {
            enabled: boolean;
        };
    };
    analytics: {
        enabled: boolean;
        postHog: {
            apiKey: string | undefined;
            host: string | undefined;
        };
    };
    application: {
        edition: EditionType;
    } | null;
    featureFlags: Record<string, boolean>;
    helpHub: {
        enabled: boolean;
        commandBar: {
            orgId: string | undefined;
        };
    };
    loading: boolean;
    signUp: {
        activationRequired: boolean;
        enabled: boolean;
    };

    getApplicationInfo: () => void;
}

const fetchGetActuatorInfo = async (): Promise<Response> => {
    return await fetch('/actuator/info', {
        method: 'GET',
    }).then((response) => response);
};

export const useApplicationInfoStore = create<ApplicationInfoI>()(
    devtools(
        (set, get) => {
            return {
                ai: {
                    copilot: {
                        enabled: false,
                    },
                },
                analytics: {
                    enabled: false,
                    postHog: {
                        apiKey: undefined,
                        host: undefined,
                    },
                },
                application: null,
                featureFlags: {},
                loading: false,
                helpHub: {
                    enabled: true,
                    commandBar: {
                        orgId: undefined,
                    },
                },
                signUp: {
                    activationRequired: false,
                    enabled: true,
                },

                getApplicationInfo: async () => {
                    if (get().loading) {
                        return;
                    }

                    set((state) => ({
                        ...state,
                        loading: true,
                    }));

                    const response = await fetchGetActuatorInfo();

                    if (response.status === 200) {
                        const json = await response.json();

                        set((state) => ({
                            ...state,
                            ai: {
                                copilot: {
                                    enabled: json.ai.copilot.enabled === 'true',
                                },
                            },
                            analytics: {
                                enabled: json.analytics.enabled === 'true',
                                postHog: json.analytics.postHog,
                            },
                            application: json.application,
                            featureFlags: json.featureFlags,
                            helpHub: {
                                enabled: json.helpHub.enabled === 'true',
                                commandBar: json.helpHub.commandBar,
                            },
                            loading: false,
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
