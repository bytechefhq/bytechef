import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useRef} from 'react';
import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';
import {useShallow} from 'zustand/react/shallow';

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
    const {featureFlags, setFeatureFlag} = useStore(featureFlagsStore, (state) => state);

    const {analytics, featureFlags: localFeatureFlags} = useApplicationInfoStore(
        useShallow((state) => ({
            analytics: state.analytics,
            featureFlags: state.featureFlags,
        }))
    );

    const loadingFlagsRef = useRef<Set<string>>(new Set());

    return (featureFlag: string): boolean => {
        // First check local feature flags from server
        if (localFeatureFlags[featureFlag] !== undefined) {
            return localFeatureFlags[featureFlag];
        }

        // Then check cached feature flags
        if (featureFlags[featureFlag] !== undefined) {
            return featureFlags[featureFlag];
        }

        // If already loading this specific flag, return current cached value
        if (loadingFlagsRef.current.has(featureFlag)) {
            return featureFlags[featureFlag] ?? false;
        }

        loadingFlagsRef.current.add(featureFlag);

        // Only try to use PostHog if analytics are enabled
        if (analytics.enabled && analytics.postHog.apiKey && analytics.postHog.host) {
            // Dynamically import PostHog only when needed
            import('posthog-js')
                .then((posthog) => {
                    const flagValue = posthog.default.getFeatureFlag(featureFlag);

                    // If flags are already loaded, use the value directly
                    if (flagValue !== undefined) {
                        setTimeout(() => {
                            setFeatureFlag(featureFlag, !!flagValue);
                            loadingFlagsRef.current.delete(featureFlag);
                        }, 0);
                    } else {
                        // Register callback for when flags finish loading
                        posthog.default.onFeatureFlags(function () {
                            setTimeout(() => {
                                setFeatureFlag(featureFlag, !!posthog.default.isFeatureEnabled(featureFlag));
                                loadingFlagsRef.current.delete(featureFlag);
                            }, 0);
                        });
                    }
                })
                .catch(() => {
                    // If PostHog fails to load, default to false
                    setTimeout(() => {
                        setFeatureFlag(featureFlag, false);
                        loadingFlagsRef.current.delete(featureFlag);
                    }, 0);
                });
        } else {
            // If analytics are disabled, default to false
            setTimeout(() => {
                setFeatureFlag(featureFlag, false);
                loadingFlagsRef.current.delete(featureFlag);
            }, 0);
        }

        return featureFlags[featureFlag] ?? false;
    };
};
