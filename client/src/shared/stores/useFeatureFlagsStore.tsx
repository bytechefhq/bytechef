import {posthog} from 'posthog-js';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface FeatureFlagsI {
    featureFlags: Record<string, boolean>;
    init: () => void;
    isFeatureFlagEnabled: (featureFlag: string) => boolean;
}

const fetchGetActuatorInfo = async (): Promise<Response> => {
    return await fetch('/actuator/info', {
        method: 'GET',
    }).then((response) => response);
};

export const useFeatureFlagsStore = create<FeatureFlagsI>()(
    devtools(
        (set, get) => {
            return {
                featureFlags: {},
                init: () => {
                    fetchGetActuatorInfo()
                        .then((response) => response.json())
                        .then((applicationInfo) => {
                            set((state) => ({
                                ...state,
                                featureFlags: {
                                    ...state.featureFlags,
                                    ...applicationInfo.featureFlags,
                                },
                            }));
                        });
                },
                isFeatureFlagEnabled: (featureFlag: string): boolean => {
                    const {featureFlags} = get();

                    if (featureFlags[featureFlag] !== undefined) {
                        return featureFlags[featureFlag];
                    }

                    set((state) => {
                        return {
                            ...state,
                            featureFlags: {
                                ...state.featureFlags,
                                [featureFlag]: false,
                            },
                        };
                    });

                    posthog.onFeatureFlags(function () {
                        if (posthog.isFeatureEnabled(featureFlag)) {
                            set((state) => {
                                return {
                                    ...state,
                                    featureFlags: {
                                        ...state.featureFlags,
                                        [featureFlag]: true,
                                    },
                                };
                            });
                        }
                    });

                    return featureFlags[featureFlag] ?? false;
                },
            };
        },
        {
            name: 'feature-flags',
        }
    )
);
