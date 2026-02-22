import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {createStore, useStore} from 'zustand';
import {devtools} from 'zustand/middleware';
import {useShallow} from 'zustand/react/shallow';

export interface FeatureFlagsI {
    featureFlags: Record<string, boolean>;
    loadingFlags: Record<string, boolean>;
    setFeatureFlag: (featureFlag: string, value: boolean) => void;
    setLoadingFlag: (featureFlag: string, loading: boolean) => void;
}

export const featureFlagsStore = createStore<FeatureFlagsI>()(
    devtools(
        (set) => {
            return {
                featureFlags: {},
                loadingFlags: {},
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
                setLoadingFlag: (featureFlag: string, loading: boolean) => {
                    set((state) => {
                        if (loading) {
                            return {
                                ...state,
                                loadingFlags: {
                                    ...state.loadingFlags,
                                    [featureFlag]: true,
                                },
                            };
                        }

                        const remainingFlags = {...state.loadingFlags};

                        delete remainingFlags[featureFlag];

                        return {
                            ...state,
                            loadingFlags: remainingFlags,
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
    const {featureFlags, setFeatureFlag} = useStore(
        featureFlagsStore,
        useShallow((state) => ({
            featureFlags: state.featureFlags,
            setFeatureFlag: state.setFeatureFlag,
        }))
    );

    const {analytics, featureFlags: localFeatureFlags} = useApplicationInfoStore(
        useShallow((state) => ({
            analytics: state.analytics,
            featureFlags: state.featureFlags,
        }))
    );

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
        if (featureFlagsStore.getState().loadingFlags[featureFlag]) {
            return featureFlags[featureFlag] ?? false;
        }

        featureFlagsStore.getState().setLoadingFlag(featureFlag, true);

        // Only try to use PostHog if analytics are enabled
        if (analytics.enabled && analytics.postHog.apiKey && analytics.postHog.host) {
            // Dynamically import PostHog only when needed
            import('posthog-js')
                .then((posthog) => {
                    // Use onFeatureFlags as the explicit "loaded" signal — it fires
                    // immediately if flags are already loaded, avoiding the unreliable
                    // getFeatureFlag() !== undefined check
                    posthog.default.onFeatureFlags(function () {
                        setTimeout(() => {
                            setFeatureFlag(featureFlag, !!posthog.default.isFeatureEnabled(featureFlag));

                            featureFlagsStore.getState().setLoadingFlag(featureFlag, false);
                        }, 0);
                    });
                })
                .catch(() => {
                    // If PostHog fails to load, default to false
                    setTimeout(() => {
                        setFeatureFlag(featureFlag, false);

                        featureFlagsStore.getState().setLoadingFlag(featureFlag, false);
                    }, 0);
                });
        } else {
            // If analytics are disabled, default to false
            setTimeout(() => {
                setFeatureFlag(featureFlag, false);

                featureFlagsStore.getState().setLoadingFlag(featureFlag, false);
            }, 0);
        }

        return featureFlags[featureFlag] ?? false;
    };
};
