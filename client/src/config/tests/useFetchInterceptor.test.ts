import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useFetchInterceptor from '../useFetchInterceptor';

const hoisted = vi.hoisted(() => {
    return {
        clearAuthentication: vi.fn(),
        clearCurrentWorkspaceId: vi.fn(),
        getCookie: vi.fn(() => 'test-xsrf-token'),
        navigate: vi.fn(),
        registeredHandlers: null as {
            request: (url: string, config: Record<string, unknown>) => unknown;
            response: (response: Record<string, unknown>) => unknown;
        } | null,
        toast: vi.fn(),
        unregister: vi.fn(),
    };
});

vi.mock('@/hooks/use-toast', () => ({
    useToast: vi.fn(() => ({
        toast: hoisted.toast,
    })),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: Record<string, unknown>) => unknown) =>
        selector({clearCurrentWorkspaceId: hoisted.clearCurrentWorkspaceId})
    ),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: Record<string, unknown>) => unknown) =>
        selector({clearAuthentication: hoisted.clearAuthentication})
    ),
}));

vi.mock('@/shared/util/cookie-utils', () => ({
    getCookie: () => hoisted.getCookie(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: vi.fn(() => hoisted.navigate),
}));

vi.mock('fetch-intercept', () => ({
    default: {
        register: vi.fn((handlers: typeof hoisted.registeredHandlers) => {
            hoisted.registeredHandlers = handlers;

            return hoisted.unregister;
        }),
    },
}));

function createMockResponse(overrides: Record<string, unknown> = {}) {
    const jsonData = overrides.jsonData ?? {};
    const jsonRejects = overrides.jsonRejects ?? false;

    return {
        clone: vi.fn(function (this: Record<string, unknown>) {
            return {
                json: vi.fn(() => (jsonRejects ? Promise.reject(new Error('Not JSON')) : Promise.resolve(jsonData))),
            };
        }),
        status: 200,
        url: 'http://localhost/internal/api/test',
        ...overrides,
    };
}

describe('useFetchInterceptor', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        hoisted.registeredHandlers = null;
        import.meta.env.VITE_API_BASE_PATH = '';
    });

    afterEach(() => {
        delete import.meta.env.VITE_API_BASE_PATH;
    });

    describe('registration', () => {
        it('registers fetch interceptor on mount', () => {
            renderHook(() => useFetchInterceptor());

            expect(hoisted.registeredHandlers).not.toBeNull();
        });

        it('unregisters interceptor on unmount', () => {
            const {unmount} = renderHook(() => useFetchInterceptor());

            unmount();

            expect(hoisted.unregister).toHaveBeenCalled();
        });
    });

    describe('request interceptor', () => {
        it('prepends apiBasePath when set', () => {
            import.meta.env.VITE_API_BASE_PATH = 'https://api.example.com';

            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/internal/test', {headers: {}});

            expect((result as string[])[0]).toBe('https://api.example.com/internal/test');
        });

        it('does not prepend apiBasePath when url already starts with it', () => {
            import.meta.env.VITE_API_BASE_PATH = 'https://api.example.com';

            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('https://api.example.com/internal/test', {
                headers: {},
            });

            expect((result as string[])[0]).toBe('https://api.example.com/internal/test');
        });

        it('adds XSRF-TOKEN header for internal URLs', () => {
            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/internal/api/test', {headers: {}});

            expect((result as [string, Record<string, unknown>])[1].headers).toEqual(
                expect.objectContaining({'X-XSRF-TOKEN': 'test-xsrf-token'})
            );
        });

        it('adds XSRF-TOKEN header for graphql URLs', () => {
            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/graphql', {headers: {}});

            expect((result as [string, Record<string, unknown>])[1].headers).toEqual(
                expect.objectContaining({'X-XSRF-TOKEN': 'test-xsrf-token'})
            );
        });

        it('does not add XSRF-TOKEN header for other URLs', () => {
            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/api/public/test', {headers: {}});

            expect((result as [string, Record<string, unknown>])[1]).toEqual({headers: {}});
        });
    });

    describe('response interceptor - authentication', () => {
        it('clears auth and navigates to login on 401', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 401, url: 'http://localhost/internal/api/test'});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).toHaveBeenCalled();
            expect(hoisted.navigate).toHaveBeenCalledWith('/login');
        });

        it('clears auth and navigates to login on 403', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 403, url: 'http://localhost/internal/api/test'});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.navigate).toHaveBeenCalledWith('/login');
        });

        it('does not navigate to login for /api/account endpoint', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 401, url: 'http://localhost/api/account'});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });
    });

    describe('response interceptor - error toast', () => {
        it('shows error toast for non-2xx responses', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {detail: 'Something went wrong', title: 'Error'},
                status: 500,
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Something went wrong',
                title: 'Error',
                variant: 'destructive',
            });
        });

        it('skips toast for AdminUserDTO with errorKey 100', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {entityClass: 'AdminUserDTO', errorKey: 100},
                status: 400,
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).not.toHaveBeenCalled();
        });

        it('shows fallback error toast when response body is not JSON', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonRejects: true,
                status: 502,
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Request failed with status 502',
                variant: 'destructive',
            });
        });
    });

    describe('response interceptor - GraphQL errors', () => {
        it('shows toast for GraphQL errors in 2xx response', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {errors: [{message: 'Field not found'}, {message: 'Permission denied'}]},
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Field not found\nPermission denied',
                title: 'GraphQL Error',
                variant: 'destructive',
            });
        });

        it('does not show toast for GraphQL response without errors', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {data: {users: []}},
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).not.toHaveBeenCalled();
        });

        it('does not check GraphQL errors for non-graphql URLs', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {errors: [{message: 'Some error'}]},
                status: 200,
                url: 'http://localhost/internal/api/test',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).not.toHaveBeenCalled();
        });

        it('handles GraphQL errors with undefined or missing message properties', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {errors: [{message: undefined}, {}, {message: 'Valid error'}]},
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Unknown error\nUnknown error\nValid error',
                title: 'GraphQL Error',
                variant: 'destructive',
            });
        });

        it('shows fallback toast when GraphQL response has invalid JSON', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonRejects: true,
                status: 500,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).toHaveBeenCalledWith({
                description: 'Request failed with status 500',
                variant: 'destructive',
            });
        });

        it('does not show toast when GraphQL 2xx response has invalid JSON', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonRejects: true,
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toast).not.toHaveBeenCalled();
        });
    });
});
