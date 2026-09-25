import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';

import type {ExtractState} from 'zustand/vanilla';

export enum EditionType {
    CE = 'CE',
    EE = 'EE',
}

export interface ApplicationInfoI {
    ai: {
        copilot: {
            enabled: boolean;
        };
        knowledgeBase: {
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
    billing: {
        customerPortalUrl: string | undefined;
        enabled: boolean;
    };
    embedded: {
        allowedParentOrigins: string[];
    } | null;
    featureFlags: Record<string, boolean>;
    helpHub: {
        commandBar: {
            orgId: string | undefined;
        };
        enabled: boolean;
    };
    loading: boolean;
    signUp: {
        activationRequired: boolean;
        enabled: boolean;
    };
    socialLogin: {
        enabled: boolean;
    };
    templatesSubmissionForm: {
        projects: string | undefined;
        workflows: string | undefined;
    };
    userGuiding: {
        containerId: string | undefined;
        enabled: boolean;
    };

    getApplicationInfo: () => Promise<void>;
}

const toAllowedParentOrigins = (value: unknown): string[] =>
    Array.isArray(value) ? value.filter((origin): origin is string => typeof origin === 'string') : [];

const fetchGetActuatorInfo = async (): Promise<Response> => {
    return await fetch('/actuator/info', {
        method: 'GET',
    }).then((response) => response);
};

export const applicationInfoStore = createStore<ApplicationInfoI>()(
    devtools(
        (set, get) => {
            return {
                ai: {
                    copilot: {
                        enabled: false,
                    },
                    knowledgeBase: {
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
                billing: {
                    customerPortalUrl: undefined,
                    enabled: false,
                },
                embedded: null,
                featureFlags: {},

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
                                knowledgeBase: {
                                    enabled: json.ai.knowledgeBase?.enabled === 'true',
                                },
                            },
                            analytics: {
                                enabled: json.analytics.enabled === 'true',
                                postHog: json.analytics.postHog,
                            },
                            application: json.application,
                            billing: {
                                customerPortalUrl: json.billing?.customerPortalUrl || undefined,
                                enabled: json.billing?.enabled === 'true',
                            },
                            embedded: {
                                allowedParentOrigins: toAllowedParentOrigins(json.embedded?.allowedParentOrigins),
                            },
                            featureFlags: json.featureFlags,
                            helpHub: {
                                commandBar: json.helpHub.commandBar,
                                enabled: json.helpHub.enabled === 'true',
                            },
                            loading: false,
                            signUp: {
                                activationRequired: json.signUp?.activationRequired === 'true',
                                enabled: json.signUp?.enabled === 'true',
                            },
                            socialLogin: {
                                enabled: json.socialLogin?.enabled === 'true',
                            },
                            templatesSubmissionForm: json.templatesSubmissionForm,
                            userGuiding: {
                                containerId: json.userGuiding?.containerId,
                                enabled: json.userGuiding?.enabled === 'true',
                            },
                        }));
                    }
                },
                helpHub: {
                    commandBar: {
                        orgId: undefined,
                    },
                    enabled: true,
                },
                loading: false,
                signUp: {
                    activationRequired: false,
                    enabled: true,
                },
                socialLogin: {
                    enabled: false,
                },
                templatesSubmissionForm: {
                    projects: undefined,
                    workflows: undefined,
                },
                userGuiding: {
                    containerId: undefined,
                    enabled: false,
                },
            };
        },
        {
            name: 'application-info',
        }
    )
);

export function useApplicationInfoStore<U>(selector: (state: ExtractState<typeof applicationInfoStore>) => U): U {
    return useStore(applicationInfoStore, selector);
}
