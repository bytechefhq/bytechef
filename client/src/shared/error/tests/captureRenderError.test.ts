import {captureRenderError} from '@/shared/error/captureRenderError';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import posthog from 'posthog-js';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mockedPosthog = posthog as unknown as {__loaded?: boolean; captureException: ReturnType<typeof vi.fn>};

const enableAnalytics = (enabled: boolean) =>
    applicationInfoStore.setState({
        analytics: {enabled, postHog: {apiKey: 'key', host: 'https://posthog.example.com'}},
    });

describe('captureRenderError', () => {
    beforeEach(() => {
        applicationInfoStore.setState(applicationInfoStore.getInitialState(), true);

        mockedPosthog.__loaded = true;
        mockedPosthog.captureException = vi.fn();
    });

    afterEach(() => {
        delete mockedPosthog.__loaded;

        vi.restoreAllMocks();
    });

    it('reports the error with its source and component stack when analytics is enabled', async () => {
        enableAnalytics(true);

        const error = new Error('render failed');

        await captureRenderError(error, {componentStack: '\n    at Widget', source: 'error_boundary'});

        expect(mockedPosthog.captureException).toHaveBeenCalledWith(error, {
            componentStack: '\n    at Widget',
            source: 'error_boundary',
        });
    });

    it('does not report when analytics is disabled', async () => {
        enableAnalytics(false);

        await captureRenderError(new Error('render failed'), {source: 'route_error_page'});

        expect(mockedPosthog.captureException).not.toHaveBeenCalled();
    });

    it('does not report when PostHog is not loaded', async () => {
        enableAnalytics(true);

        delete mockedPosthog.__loaded;

        await captureRenderError(new Error('render failed'), {source: 'route_error_page'});

        expect(mockedPosthog.captureException).not.toHaveBeenCalled();
    });

    it('warns instead of throwing when PostHog fails to report', async () => {
        enableAnalytics(true);

        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        mockedPosthog.captureException = vi.fn(() => {
            throw new Error('network down');
        });

        await expect(
            captureRenderError(new Error('render failed'), {source: 'error_boundary'})
        ).resolves.toBeUndefined();

        expect(consoleWarnSpy).toHaveBeenCalledWith('Could not report the render error:', expect.any(Error));
    });
});
