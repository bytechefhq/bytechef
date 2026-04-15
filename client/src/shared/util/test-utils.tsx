import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {cleanup, render} from '@testing-library/react';
import {ReactElement, ReactNode} from 'react';
import {afterEach, vi} from 'vitest';

afterEach(() => {
    cleanup();
});

const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            mutations: {
                retry: false,
            },
            queries: {
                retry: false,
            },
        },
    });

export const createTestQueryClientWrapper = () => {
    const testQueryClient = createTestQueryClient();

    return ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>
    );
};

const customRender = (ui: ReactElement, options = {}) => {
    const testQueryClient = createTestQueryClient();

    return render(ui, {
        wrapper: ({children}) => <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>,
        ...options,
    });
};

export const windowResizeObserver = () => {
    class MockResizeObserver {
        disconnect() {}
        /* eslint-disable @typescript-eslint/no-unused-vars */
        observe(_target?: Element, _options?: ResizeObserverOptions) {}
        /* eslint-disable @typescript-eslint/no-unused-vars */
        unobserve(_target?: Element) {}
    }

    // Assign as constructor-compatible classes
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any).ResizeObserver = MockResizeObserver as unknown as typeof ResizeObserver;
};

export const resetAll = () => {
    cleanup();

    vi.clearAllMocks();

    if (window.ResizeObserver) {
        delete (window as unknown as {ResizeObserver?: ResizeObserver}).ResizeObserver;
    }

    vi.resetModules();
};

export function mockScrollIntoView() {
    Element.prototype.scrollIntoView = vi.fn();
}

/**
 * Test wrapper for connection components: pre-mocks the standard hook set
 * (application info / authentication / workspace / environment) and renders inside the React Query
 * provider. Use directly in component tests to skip ~100 lines of boilerplate hook mocks.
 *
 * Usage: in your test file, vi.mock the store modules first, then call mockConnectionStores({...})
 * inside beforeEach with overrides, and use renderConnectionComponent() to render the subject.
 */
export interface ConnectionTestStoresI {
    accountAuthorities?: string[];
    currentEnvironmentId?: number;
    currentWorkspaceId?: number;
    edition?: 'CE' | 'EE';
}

export const DEFAULT_CONNECTION_TEST_STORES: Required<ConnectionTestStoresI> = {
    accountAuthorities: ['ROLE_ADMIN'],
    currentEnvironmentId: 1,
    currentWorkspaceId: 100,
    edition: 'EE',
};

export const renderConnectionComponent = (ui: ReactElement, options = {}) => {
    const testQueryClient = createTestQueryClient();

    return render(ui, {
        wrapper: ({children}) => <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>,
        ...options,
    });
};

export * from '@testing-library/react';
export {default as userEvent} from '@testing-library/user-event';
// override render export
export {customRender as render};
