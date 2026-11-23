import ErrorPage from '@/shared/error/ErrorPage';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {render, screen, waitFor} from '@/shared/util/test-utils';
import posthog from 'posthog-js';
import {ReactElement} from 'react';
import {RouterProvider, createMemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mockedPosthog = posthog as unknown as {__loaded?: boolean; captureException: ReturnType<typeof vi.fn>};

const ThrowingComponent = (): never => {
    throw new Error("Failed to execute 'removeChild' on 'Node'");
};

const enableAnalytics = (enabled: boolean) =>
    applicationInfoStore.setState({
        analytics: {enabled, postHog: {apiKey: 'key', host: 'https://posthog.example.com'}},
    });

const renderRouteWithElement = (element: ReactElement) =>
    render(<RouterProvider router={createMemoryRouter([{element, errorElement: <ErrorPage />, path: '/'}])} />);

const renderRouteWithLoaderResponse = (status: number) =>
    render(
        <RouterProvider
            router={createMemoryRouter([
                {
                    element: <div />,
                    errorElement: <ErrorPage />,
                    loader: () => {
                        throw new Response('', {status});
                    },
                    path: '/',
                },
            ])}
        />
    );

describe('ErrorPage', () => {
    beforeEach(() => {
        applicationInfoStore.setState(applicationInfoStore.getInitialState(), true);

        mockedPosthog.__loaded = true;
        mockedPosthog.captureException = vi.fn();

        vi.spyOn(console, 'error').mockImplementation(() => {});
        vi.spyOn(console, 'warn').mockImplementation(() => {});
    });

    afterEach(() => {
        delete mockedPosthog.__loaded;

        vi.restoreAllMocks();
    });

    it('reports a route render error to PostHog when analytics is enabled', async () => {
        enableAnalytics(true);

        renderRouteWithElement(<ThrowingComponent />);

        expect(await screen.findByText('Oops, something went wrong!')).toBeInTheDocument();

        await waitFor(() => {
            expect(mockedPosthog.captureException).toHaveBeenCalledWith(
                expect.objectContaining({message: "Failed to execute 'removeChild' on 'Node'"}),
                expect.objectContaining({source: 'route_error_page'})
            );
        });
    });

    it('does not report a route render error when analytics is disabled', async () => {
        enableAnalytics(false);

        renderRouteWithElement(<ThrowingComponent />);

        expect(await screen.findByText('Oops, something went wrong!')).toBeInTheDocument();

        await vi.dynamicImportSettled();

        expect(mockedPosthog.captureException).not.toHaveBeenCalled();
    });

    it('does not report a thrown route response', async () => {
        enableAnalytics(true);

        renderRouteWithLoaderResponse(404);

        expect(await screen.findByText('Oops, something went wrong!')).toBeInTheDocument();

        await vi.dynamicImportSettled();

        expect(mockedPosthog.captureException).not.toHaveBeenCalled();
    });
});
