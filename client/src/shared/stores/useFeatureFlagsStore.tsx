import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useRef} from 'react';
import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface FeatureFlagsI {
    featureFlags: Record<string, boolean>;
    setFeatureFlag: (featureFlag: string, value: boolean) => void;
}

const featureFlagsStore = createStore<FeatureFlagsI>()(
    devtools(
        (set) => {
            return {
                featureFlags: {},
                setFeatureFlag: (featureFlag: string, value: boolean) => {
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

    const {featureFlags, setFeatureFlag} = useStore(featureFlagsStore, (state) => state);

    const {analytics, featureFlags: localFeatureFlags} = useApplicationInfoStore();

    return (featureFlag: string): boolean => {
        if (loadingRef.current) {
            return false;
        }

        loadingRef.current = true;

        // First check local feature flags from server
        if (localFeatureFlags[featureFlag] !== undefined) {
            loadingRef.current = false;

            return localFeatureFlags[featureFlag];
        }

        // Then check cached feature flags
        if (featureFlags[featureFlag] !== undefined) {
            loadingRef.current = false;

            return featureFlags[featureFlag];
        }

        // Only try to use PostHog if analytics are enabled
        if (analytics.enabled && analytics.postHog.apiKey && analytics.postHog.host) {
            // Dynamically import PostHog only when needed
            import('posthog-js')
                .then((posthog) => {
                    posthog.default.onFeatureFlags(function () {
                        if (posthog.default.isFeatureEnabled(featureFlag)) {
                            setFeatureFlag(featureFlag, true);
                        } else {
                            setFeatureFlag(featureFlag, false);
                        }

                        loadingRef.current = false;
                    });
                })
                .catch(() => {
                    // If PostHog fails to load, default to false
                    setFeatureFlag(featureFlag, false);

                    loadingRef.current = false;
                });
        } else {
            // If analytics are disabled, default to false
            setFeatureFlag(featureFlag, false);

            loadingRef.current = false;
        }

        return featureFlags[featureFlag] ?? false;
    };
};
