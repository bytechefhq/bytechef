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
        gateway: {
            enabled: boolean;
        };
        hub: {
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
        enabled: boolean;
    };
    component: {
        customComponent: {
            javaEnabled: boolean;
        };
    };
    contextStore: {
        enabled: boolean;
    };
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
    workflow: {
        codeWorkflow: {
            javaEnabled: boolean;
        };
    };

    getApplicationInfo: () => Promise<void>;
}

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
                    gateway: {
                        enabled: false,
                    },
                    hub: {
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
                    enabled: false,
                },
                component: {
                    customComponent: {
                        javaEnabled: true,
                    },
                },
                contextStore: {
                    enabled: false,
                },
                featureFlags: {},

                getApplicationInfo: async () => {
                    if (get().loading) {
                        return;
                    }

                    set((state) => ({
                        ...state,
                        loading: true,
                    }));

                    let response: Response;

                    try {
                        response = await fetchGetActuatorInfo();
                    } catch (error) {
                        set((state) => ({...state, loading: false}));

                        throw error;
                    }

                    if (response.status !== 200) {
                        set((state) => ({...state, loading: false}));

                        return;
                    }

                    if (response.status === 200) {
                        const json = await response.json();

                        set((state) => ({
                            ...state,
                            ai: {
                                copilot: {
                                    enabled: json.ai.copilot.enabled === 'true',
                                },
                                gateway: {
                                    enabled: json.ai.gateway?.enabled === 'true',
                                },
                                hub: {
                                    enabled: json.ai.hub?.enabled === 'true',
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
                                enabled: json.billing?.enabled === 'true',
                            },
                            component: {
                                customComponent: {
                                    javaEnabled: json.component?.customComponent?.javaEnabled !== 'false',
                                },
                            },
                            contextStore: {
                                enabled: json.contextStore?.enabled === 'true',
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
                            workflow: {
                                codeWorkflow: {
                                    javaEnabled: json.workflow?.codeWorkflow?.javaEnabled !== 'false',
                                },
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
                workflow: {
                    codeWorkflow: {
                        javaEnabled: true,
                    },
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
