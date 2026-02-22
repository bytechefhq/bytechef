import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {featureFlagsStore, useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {act, renderHook} from '@testing-library/react';
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

function flushAsync(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, 10));
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
        it('marks flag as loading in the zustand store', () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-300');

            expect(featureFlagsStore.getState().loadingFlags['ff-300']).toBe(true);
        });

        it('prevents duplicate fetches for the same flag', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-300');
            result.current('ff-300');
            result.current('ff-300');

            await act(async () => {
                await flushAsync();
            });

            expect(vi.mocked(posthog.onFeatureFlags)).toHaveBeenCalledTimes(1);
        });

        it('allows concurrent loading of different flags independently', () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-300');
            result.current('ff-301');
            result.current('ff-302');

            const {loadingFlags} = featureFlagsStore.getState();

            expect(loadingFlags['ff-300']).toBe(true);
            expect(loadingFlags['ff-301']).toBe(true);
            expect(loadingFlags['ff-302']).toBe(true);
        });

        it('shares loading state across multiple hook instances', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            const {result: hookInstance1} = renderHook(() => useFeatureFlagsStore());
            const {result: hookInstance2} = renderHook(() => useFeatureFlagsStore());

            hookInstance1.current('ff-shared');
            hookInstance2.current('ff-shared');

            await act(async () => {
                await flushAsync();
            });

            expect(vi.mocked(posthog.onFeatureFlags)).toHaveBeenCalledTimes(1);
        });
    });

    describe('analytics disabled path', () => {
        it('defaults unknown flag to false when analytics are disabled', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            expect(result.current('ff-400')).toBe(false);

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().featureFlags['ff-400']).toBe(false);
        });

        it('clears loading flag after resolving', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            expect(featureFlagsStore.getState().loadingFlags['ff-400']).toBe(true);

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().loadingFlags['ff-400']).toBeUndefined();
        });

        it('does not attempt PostHog import when analytics disabled', async () => {
            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            await act(async () => {
                await flushAsync();
            });

            expect(vi.mocked(posthog.onFeatureFlags)).not.toHaveBeenCalled();
        });

        it('does not attempt PostHog import when apiKey is missing', async () => {
            applicationInfoStore.setState({
                analytics: {enabled: true, postHog: {apiKey: undefined, host: 'https://test.com'}},
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            await act(async () => {
                await flushAsync();
            });

            expect(vi.mocked(posthog.onFeatureFlags)).not.toHaveBeenCalled();
            expect(featureFlagsStore.getState().featureFlags['ff-400']).toBe(false);
        });

        it('does not attempt PostHog import when host is missing', async () => {
            applicationInfoStore.setState({
                analytics: {enabled: true, postHog: {apiKey: 'phc_test', host: undefined}},
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-400');

            await act(async () => {
                await flushAsync();
            });

            expect(vi.mocked(posthog.onFeatureFlags)).not.toHaveBeenCalled();
            expect(featureFlagsStore.getState().featureFlags['ff-400']).toBe(false);
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

            await act(async () => {
                await flushAsync();
            });

            expect(posthog.onFeatureFlags).toHaveBeenCalled();
            expect(posthog.isFeatureEnabled).toHaveBeenCalledWith('ff-500');
            expect(featureFlagsStore.getState().featureFlags['ff-500']).toBe(true);
        });

        it('sets flag to false when isFeatureEnabled returns false', async () => {
            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                (callback as () => void)();

                return () => {};
            });

            vi.mocked(posthog.isFeatureEnabled).mockReturnValue(false);

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-500');

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().featureFlags['ff-500']).toBe(false);
        });

        it('clears loading flag after PostHog resolves', async () => {
            vi.mocked(posthog.onFeatureFlags).mockImplementation((callback) => {
                (callback as () => void)();

                return () => {};
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-500');

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().loadingFlags['ff-500']).toBeUndefined();
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

            await act(async () => {
                await flushAsync();
            });

            // Flag should still be loading
            expect(featureFlagsStore.getState().loadingFlags['ff-deferred']).toBe(true);
            expect(featureFlagsStore.getState().featureFlags['ff-deferred']).toBeUndefined();

            // Now fire the deferred callback
            capturedCallback!();

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().featureFlags['ff-deferred']).toBe(true);
            expect(featureFlagsStore.getState().loadingFlags['ff-deferred']).toBeUndefined();
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

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().featureFlags['ff-600']).toBe(false);
        });

        it('clears loading flag after PostHog failure', async () => {
            applicationInfoStore.setState({analytics: analyticsEnabled});

            vi.mocked(posthog.onFeatureFlags).mockImplementation(() => {
                throw new Error('PostHog error');
            });

            const {result} = renderHook(() => useFeatureFlagsStore());

            result.current('ff-600');

            await act(async () => {
                await flushAsync();
            });

            expect(featureFlagsStore.getState().loadingFlags['ff-600']).toBeUndefined();
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
