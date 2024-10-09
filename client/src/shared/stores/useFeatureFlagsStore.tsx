import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {posthog} from 'posthog-js';
import {useRef} from 'react';
import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface FeatureFlagsI {
    featureFlags: Record<string, boolean>;
    set: (featureFlag: string, value: boolean) => void;
}

const featureFlagsStore = createStore<FeatureFlagsI>()(
    devtools(
        (set) => {
            return {
                featureFlags: {},
                set: (featureFlag: string, value: boolean) => {
                    set((state) => {
                        return {
                            ...state,
                            featureFlags: {
                                ...state.featureFlags,
                                [featureFlag]: value,
                            },
                        };
                    });
                },
            };
        },
        {
            name: 'feature-flags',
        }
    )
);

export const useFeatureFlagsStore = (): ((featureFlag: string) => boolean) => {
    const loadingRef = useRef(false);

    const {featureFlags, set} = useStore(featureFlagsStore, (state) => state);

    const {featureFlags: localFeatureFlags} = useApplicationInfoStore();

    return (featureFlag: string): boolean => {
        if (loadingRef.current) {
            return false;
        }

        loadingRef.current = true;

        if (localFeatureFlags[featureFlag] !== undefined) {
            loadingRef.current = false;

            return localFeatureFlags[featureFlag];
        }

        if (featureFlags[featureFlag] !== undefined) {
            loadingRef.current = false;

            return featureFlags[featureFlag];
        }

        posthog.onFeatureFlags(function () {
            if (posthog.isFeatureEnabled(featureFlag)) {
                set(featureFlag, true);
            } else {
                set(featureFlag, false);
            }

            loadingRef.current = false;
        });

        return featureFlags[featureFlag] ?? false;
    };
};
