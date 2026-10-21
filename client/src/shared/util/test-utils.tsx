import {ThemeProvider} from '@/shared/providers/theme-provider';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {cleanup, render} from '@testing-library/react';
import {ReactElement, ReactNode} from 'react';
import {type Mock, afterEach, vi} from 'vitest';

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
        wrapper: ({children}) => (
            <ThemeProvider>
                <QueryClientProvider client={testQueryClient}>{children}</QueryClientProvider>
            </ThemeProvider>
        ),
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

interface MutationMockOptionsI<TData, TVariables> {
    onError?: (error: unknown, variables: TVariables) => void;
    onSuccess?: (data: TData, variables: TVariables) => void;
}

/**
 * Wires a stable stand-in implementation onto an already-mocked generated `useXMutation` GraphQL
 * hook.
 *
 * The naive `vi.mock('@/shared/middleware/graphql', () => ({useXMutation: () => ({isPending:
 * false, mutate: vi.fn()})}))` creates a BRAND NEW `mutate` function every time the hook is called
 * (i.e. on every render), which both breaks any `useEffect` keyed on the mutation and makes
 * `expect(mutate).toHaveBeenCalled()` silently meaningless — the assertion only ever sees whichever
 * `mutate` instance happened to run last. This helper keeps `mutate` at one stable identity across
 * renders and captures the latest `onSuccess`/`onError` the component passed in, so a test can
 * trigger them manually to exercise the onSuccess -> invalidateQueries -> onClose chain.
 *
 * `vi.mock` factories hoist above module-scope `const` declarations (see CLAUDE.md's "Vitest mock
 * factory hoisting"), so this can't be called from inside the factory itself. Instead, mock the
 * hook as a bare `vi.fn()` in the factory, grab it back via `await import(...)`, and call this
 * helper on the result — the same shape already used by `useTruncateTaskMessages.test.tsx`.
 *
 * Usage:
 * ```ts
 * vi.mock('@/shared/middleware/graphql', () => ({
 *     useCreateWorkflowAlertRuleMutation: vi.fn(),
 * }));
 *
 * const {useCreateWorkflowAlertRuleMutation} = await import('@/shared/middleware/graphql');
 *
 * const createRuleMutation = stubMutation(vi.mocked(useCreateWorkflowAlertRuleMutation));
 *
 * // in a test:
 * expect(createRuleMutation.mutate).toHaveBeenCalledWith({input, workspaceId: '1'});
 * createRuleMutation.triggerOnSuccess(undefined, {input, workspaceId: '1'});
 * expect(onClose).toHaveBeenCalledTimes(1);
 * ```
 */
export const stubMutation = <TData = unknown, TVariables = unknown>(
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mutationHookMock: Mock<(...args: any[]) => any>
) => {
    let latestOnError: MutationMockOptionsI<TData, TVariables>['onError'];
    let latestOnSuccess: MutationMockOptionsI<TData, TVariables>['onSuccess'];

    const mutate = vi.fn();

    mutationHookMock.mockImplementation((options?: MutationMockOptionsI<TData, TVariables>) => {
        latestOnError = options?.onError;
        latestOnSuccess = options?.onSuccess;

        return {isPending: false, mutate};
    });

    const triggerOnError = (error: unknown, variables: TVariables) => {
        latestOnError?.(error, variables);
    };

    const triggerOnSuccess = (data: TData, variables: TVariables) => {
        latestOnSuccess?.(data, variables);
    };

    return {mutate, triggerOnError, triggerOnSuccess};
};

export * from '@testing-library/react';
export {default as userEvent} from '@testing-library/user-event';
// override render export
export {customRender as render};
