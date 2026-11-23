import ErrorBoundary from '@/shared/error/ErrorBoundary';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import posthog from 'posthog-js';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mockedPosthog = posthog as unknown as {__loaded?: boolean; captureException: ReturnType<typeof vi.fn>};

const ThrowingComponent = (): never => {
    throw new Error("Failed to execute 'removeChild' on 'Node'");
};

const enableAnalytics = (enabled: boolean) =>
    applicationInfoStore.setState({
        analytics: {enabled, postHog: {apiKey: 'key', host: 'https://posthog.example.com'}},
    });

describe('ErrorBoundary', () => {
    beforeEach(() => {
        applicationInfoStore.setState(applicationInfoStore.getInitialState(), true);

        mockedPosthog.__loaded = true;
        mockedPosthog.captureException = vi.fn();

        vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        delete mockedPosthog.__loaded;

        vi.restoreAllMocks();
    });

    it('reports the caught error to PostHog when analytics is enabled', async () => {
        enableAnalytics(true);

        render(
            <ErrorBoundary>
                <ThrowingComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('Something went wrong')).toBeInTheDocument();

        await waitFor(() => {
            expect(mockedPosthog.captureException).toHaveBeenCalledWith(
                expect.objectContaining({message: "Failed to execute 'removeChild' on 'Node'"}),
                expect.objectContaining({source: 'error_boundary'})
            );
        });
    });

    it('does not report the caught error when analytics is disabled', () => {
        enableAnalytics(false);

        render(
            <ErrorBoundary>
                <ThrowingComponent />
            </ErrorBoundary>
        );

        expect(screen.getByText('Something went wrong')).toBeInTheDocument();
        expect(mockedPosthog.captureException).not.toHaveBeenCalled();
    });
});
