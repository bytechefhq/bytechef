import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {featureFlagsStore, useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {act, renderHook, waitFor} from '@testing-library/react';
import posthog from 'posthog-js';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const analyticsEnabled = {
    enabled: true,
    postHog: {apiKey: 'phc_test', host: 'https://test.posthog.com'},
};

const analyticsDisabled = {
    enabled: false,
    postHog: {apiKey: undefined, host: undefined},
};

// The store resolves a flag through a four-hop async chain: dynamic import('posthog-js') ->
// promise microtask -> onFeatureFlags callback -> setTimeout(..., 0). Its latency is unbounded
// under parallel vitest workers, so every wait below polls for the condition it actually cares
// about instead of sleeping for a fixed number of milliseconds. A fixed sleep raced the chain
// and made this file flaky in full runs while passing in isolation.
//
// The chain settles in ~1-2ms when a worker is scheduled, so poll tightly; the timeout is a
// generous ceiling paid only when a test genuinely fails, not a delay every test waits out.
const WAIT_OPTIONS = {interval: 5, timeout: 5000};

function waitForFlagValue(featureFlag: string, value: boolean): Promise<void> {
    return waitFor(() => {
        expect(featureFlagsStore.getState().featureFlags[featureFlag]).toBe(value);
    }, WAIT_OPTIONS);
}

function waitForFlagSettled(featureFlag: string): Promise<void> {
    return waitFor(() => {
        expect(featureFlagsStore.getState().loadingFlags[featureFlag]).toBeUndefined();
    }, WAIT_OPTIONS);
}

function waitForPostHogSubscription(times: number): Promise<void> {
    return waitFor(() => {
        expect(vi.mocked(posthog.onFeatureFlags)).toHaveBeenCalledTimes(times);
    }, WAIT_OPTIONS);
}

describe('useFeatureFlagsStore', () => {
    beforeEach(() => {
        featureFlagsStore.setState({
            featureFlags: {},
            loadingFlags: {},
        });

        applicationInfoStore.setState({
            analytics: analyticsDisabled,
            featureFlags: {},
        });

        vi.mocked(posthog.onFeatureFlags).mockReset();
        vi.mocked(posthog.isFeatureEnabled).mockReset().mockReturnValue(false);
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('local feature flags', () => {
        it('returns true for a local flag set to true', () => {
            applicationInfoStore.setState({featureFlags: {'ff-100': true}});

            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-100')).toBe(true);
        });

        it('returns false for a local flag set to false', () => {
            applicationInfoStore.setState({featureFlags: {'ff-100': false}});

            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-100')).toBe(false);
        });

        it('prioritizes local flags over cached PostHog flags', () => {
            applicationInfoStore.setState({featureFlags: {'ff-100': false}});
            featureFlagsStore.setState({featureFlags: {'ff-100': true}});

            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-100')).toBe(false);
        });
    });

    describe('cached feature flags', () => {
        it('returns cached flag value when no local flag exists', () => {
            featureFlagsStore.setState({featureFlags: {'ff-200': true}});

            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-200')).toBe(true);
        });

        it('returns false for a cached flag set to false', () => {
            featureFlagsStore.setState({featureFlags: {'ff-200': false}});

            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-200')).toBe(false);
        });

        it('does not trigger PostHog loading for cached flags', () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});
            featureFlagsStore.setState({featureFlags: {'ff-200': true}});

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-200');

            expect(featureFlagsStore.getState().loadingFlags['ff-200']).toBeUndefined();
        });
    });

    describe('per-flag loading guard', () => {
        it('marks flag as loading in the zustand store', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-300');

            expect(featureFlagsStore.getState().loadingFlags['ff-300']).toBe(true);

            // Drain the resolution this test started, so its timers cannot fire during a later test.
            await waitForFlagSettled('ff-300');
        });

        it('prevents duplicate fetches for the same flag', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-300');
            result.current('ff-300');
            result.current('ff-300');

            await waitForPostHogSubscription(1);
        });

        it('allows concurrent loading of different flags independently', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-300');
            result.current('ff-301');
            result.current('ff-302');

            const {loadingFlags} = featureFlagsStore.getState();

            expect(loadingFlags['ff-300']).toBe(true);
            expect(loadingFlags['ff-301']).toBe(true);
            expect(loadingFlags['ff-302']).toBe(true);

            // Drain all three resolutions before the test ends.
            await waitForFlagSettled('ff-300');
            await waitForFlagSettled('ff-301');
            await waitForFlagSettled('ff-302');
        });

        it('shares loading state across multiple hook instances', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            const {result: hookInstance1} = renderHook(() => useFeatureFlagsStore());
            const {result: hookInstance2} = renderHook(() => useFeatureFlagsStore());

            hookInstance1.current('ff-shared');
            hookInstance2.current('ff-shared');

            await waitForPostHogSubscription(1);
        });
    });

    describe('analytics disabled path', () => {
        it('defaults unknown flag to false when analytics are disabled', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-400')).toBe(false);

            await waitForFlagValue('ff-400', false);
        });

        it('clears loading flag after resolving', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            expect(featureFlagsStore.getState().loadingFlags['ff-400']).toBe(true);

            await waitForFlagSettled('ff-400');
        });

        it('does not attempt PostHog import when analytics disabled', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            // Wait for the resolution to complete, then assert PostHog was never reached.
            await waitForFlagValue('ff-400', false);

            expect(vi.mocked(posthog.onFeatureFlags)).not.toHaveBeenCalled();
        });

        it('does not attempt PostHog import when apiKey is missing', async () => {
            applicationInfoStore.setState({
                analytics: {enabled: true, postHog: {apiKey: undefined, host: 'https://test.com'}},
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            await waitForFlagValue('ff-400', false);

            expect(vi.mocked(posthog.onFeatureFlags)).not.toHaveBeenCalled();
        });

        it('does not attempt PostHog import when host is missing', async () => {
            applicationInfoStore.setState({
                analytics: {enabled: true, postHog: {apiKey: 'phc_test', host: undefined}},
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            await waitForFlagValue('ff-400', false);

            expect(vi.mocked(posthog.onFeatureFlags)).not.toHaveBeenCalled();
        });
    });

    describe('PostHog integration', () => {
        beforeEach(() => {
            applicationInfoStore.setState({analytics: analyticsEnabled});
        });

        it('uses onFeatureFlags callback to detect when flags are loaded', async () => {
            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                (callback as () => void)();

                return () => {};
            });

            vi.mocked(posthog.isFeatureEnabled).mockReturnValue(true);

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-500');

            await waitForFlagValue('ff-500', true);

            expect(posthog.onFeatureFlags).toHaveBeenCalled();
            expect(posthog.isFeatureEnabled).toHaveBeenCalledWith('ff-500');
        });

        it('sets flag to false when isFeatureEnabled returns false', async () => {
            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                (callback as () => void)();

                return () => {};
            });

            vi.mocked(posthog.isFeatureEnabled).mockReturnValue(false);

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-500');

            await waitForFlagValue('ff-500', false);
        });

        it('clears loading flag after PostHog resolves', async () => {
            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                (callback as () => void)();

                return () => {};
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-500');

            expect(featureFlagsStore.getState().loadingFlags['ff-500']).toBe(true);

            await waitForFlagSettled('ff-500');
        });

        it('handles deferred onFeatureFlags callback', async () => {
            let capturedCallback: (() => void) | undefined;

            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                capturedCallback = callback as () => void;

                return () => {};
            });

            vi.mocked(posthog.isFeatureEnabled).mockReturnValue(true);

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-deferred');

            // Wait for the subscription rather than a fixed delay; until the captured callback
            // fires, the store cannot resolve the flag, so the loading state below is stable.
            await waitFor(() => {
                expect(capturedCallback).toBeDefined();
            }, WAIT_OPTIONS);

            expect(featureFlagsStore.getState().loadingFlags['ff-deferred']).toBe(true);
            expect(featureFlagsStore.getState().featureFlags['ff-deferred']).toBeUndefined();

            // Now fire the deferred callback
            act(() => {
                capturedCallback!();
            });

            await waitForFlagValue('ff-deferred', true);

            await waitForFlagSettled('ff-deferred');
        });

        it('unsubscribes from onFeatureFlags after first invocation to prevent leaks', async () => {
            const unsubscribe = vi.fn();

            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                (callback as () => void)();

                return unsubscribe;
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-unsub');

            await waitFor(() => {
                expect(unsubscribe).toHaveBeenCalledTimes(1);
            }, WAIT_OPTIONS);
        });
    });

    describe('PostHog import failure', () => {
        it('defaults flag to false when PostHog throws during onFeatureFlags', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            vi.mocked(posthog.onFeatureFlags).mockImplementation(() => {
                throw new Error('PostHog error');
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-600');

            await waitForFlagValue('ff-600', false);
        });

        it('clears loading flag after PostHog failure', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            vi.mocked(posthog.onFeatureFlags).mockImplementation(() => {
                throw new Error('PostHog error');
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-600');

            expect(featureFlagsStore.getState().loadingFlags['ff-600']).toBe(true);

            await waitForFlagSettled('ff-600');
        });
    });

    describe('store actions', () => {
        it('setFeatureFlag adds a flag to the store', () => {
            act(() => {
                featureFlagsStore.getState().setFeatureFlag('ff-700', true);
            });

            expect(featureFlagsStore.getState().featureFlags['ff-700']).toBe(true);
        });

        it('setFeatureFlag overwrites an existing flag', () => {
            featureFlagsStore.setState({featureFlags: {'ff-700': true}});

            act(() => {
                featureFlagsStore.getState().setFeatureFlag('ff-700', false);
            });

            expect(featureFlagsStore.getState().featureFlags['ff-700']).toBe(false);
        });

        it('setFeatureFlag preserves other flags', () => {
            featureFlagsStore.setState({featureFlags: {'ff-700': true, 'ff-701': false}});

            act(() => {
                featureFlagsStore.getState().setFeatureFlag('ff-700', false);
            });

            expect(featureFlagsStore.getState().featureFlags['ff-700']).toBe(false);
            expect(featureFlagsStore.getState().featureFlags['ff-701']).toBe(false);
        });

        it('setLoadingFlag marks a flag as loading', () => {
            act(() => {
                featureFlagsStore.getState().setLoadingFlag('ff-700', true);
            });

            expect(featureFlagsStore.getState().loadingFlags['ff-700']).toBe(true);
        });

        it('setLoadingFlag removes a flag from loading', () => {
            featureFlagsStore.setState({loadingFlags: {'ff-700': true}});

            act(() => {
                featureFlagsStore.getState().setLoadingFlag('ff-700', false);
            });

            expect(featureFlagsStore.getState().loadingFlags['ff-700']).toBeUndefined();
        });

        it('setLoadingFlag does not affect other loading flags', () => {
            featureFlagsStore.setState({loadingFlags: {'ff-700': true, 'ff-701': true}});

            act(() => {
                featureFlagsStore.getState().setLoadingFlag('ff-700', false);
            });

            expect(featureFlagsStore.getState().loadingFlags['ff-700']).toBeUndefined();
            expect(featureFlagsStore.getState().loadingFlags['ff-701']).toBe(true);
        });

        it('store state is resettable for tests', () => {
            featureFlagsStore.setState({
                featureFlags: {'ff-700': true},
                loadingFlags: {'ff-701': true},
            });

            featureFlagsStore.setState({
                featureFlags: {},
                loadingFlags: {},
            });

            expect(featureFlagsStore.getState().featureFlags).toEqual({});
            expect(featureFlagsStore.getState().loadingFlags).toEqual({});
        });
    });
});
