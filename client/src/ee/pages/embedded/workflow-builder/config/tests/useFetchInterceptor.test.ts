import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useFetchInterceptor, {clearActiveToasts} from '../useFetchInterceptor';

const hoisted = vi.hoisted(() => {
    return {
        clearAuthentication: vi.fn(),
        clearCurrentEnvironmentId: vi.fn(),
        clearCurrentWorkspaceId: vi.fn(),
        registeredHandlers: null as {
            request: (url: string, config: Record<string, unknown>) => unknown;
            response: (response: Record<string, unknown>) => unknown;
        } | null,
        toastError: vi.fn(),
        unregister: vi.fn(),
    };
});

vi.mock('sonner', () => ({
    toast: {
        error: hoisted.toastError,
    },
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

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector: (state: Record<string, unknown>) => unknown) =>
        selector({clearCurrentEnvironmentId: hoisted.clearCurrentEnvironmentId})
    ),
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

describe('useFetchInterceptor (embedded)', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        clearActiveToasts();
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
        it('adds Authorization and X-ENVIRONMENT headers for internal URLs', () => {
            sessionStorage.setItem('jwtToken', 'test-jwt');
            sessionStorage.setItem('environment', 'production');

            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/internal/test', {headers: {}});

            expect((result as [string, Record<string, unknown>])[1].headers).toEqual(
                expect.objectContaining({
                    Authorization: 'Bearer test-jwt',
                    'X-ENVIRONMENT': 'PRODUCTION',
                })
            );

            sessionStorage.removeItem('jwtToken');
            sessionStorage.removeItem('environment');
        });

        it('does not add auth headers for non-internal URLs', () => {
            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/api/public/test', {headers: {}});

            expect((result as [string, Record<string, unknown>])[1]).toEqual({headers: {}});
        });
    });

    describe('response interceptor - authentication', () => {
        it('clears auth state on 401', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 401});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.clearCurrentEnvironmentId).toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).toHaveBeenCalled();
        });

        it('clears auth state on 403', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 403});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.clearCurrentEnvironmentId).toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).toHaveBeenCalled();
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

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', {
                description: 'Something went wrong',
                id: 'fetch-error-500',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
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

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('suppresses duplicate error toasts while toast is active', async () => {
            renderHook(() => useFetchInterceptor());

            const createResponse = () =>
                createMockResponse({
                    jsonData: {detail: 'Something went wrong', title: 'Error'},
                    status: 500,
                });

            hoisted.registeredHandlers!.response(createResponse());

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledTimes(1);

            hoisted.registeredHandlers!.response(createResponse());

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledTimes(1);
        });

        it('allows new toast after previous toast is dismissed', async () => {
            renderHook(() => useFetchInterceptor());

            const createResponse = () =>
                createMockResponse({
                    jsonData: {detail: 'Something went wrong', title: 'Error'},
                    status: 500,
                });

            hoisted.registeredHandlers!.response(createResponse());

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledTimes(1);

            const onDismiss = hoisted.toastError.mock.calls[0][1].onDismiss;

            onDismiss();

            hoisted.registeredHandlers!.response(createResponse());

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledTimes(2);
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

            expect(hoisted.toastError).toHaveBeenCalledWith('Request failed with status 502', {
                id: 'fetch-error-502',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
            });
        });
    });

    describe('workflow node lookups', () => {
        const lookupUrl = 'http://localhost/internal/workflows/1052/workflow-nodes/dataTable_2/options/table';

        beforeEach(() => {
            useWorkflowIssuesStore.getState().reset();
        });

        it('records a failed lookup against the node instead of toasting', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {detail: "Table does not have primary key column 'id': dt_0_conversations", title: 'Error'},
                status: 500,
                url: lookupUrl,
            });

            await act(async () => {
                hoisted.registeredHandlers!.response(response);

                await Promise.resolve();
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
            expect(useWorkflowIssuesStore.getState().liveIssues['dataTable_2|table|LOOKUP_FAILED'].message).toBe(
                "Table does not have primary key column 'id': dt_0_conversations"
            );
        });

        it('clears the recorded failure when the same lookup succeeds', async () => {
            useWorkflowIssuesStore.getState().recordLookupFailure('dataTable_2', 'table', 'stale');

            renderHook(() => useFetchInterceptor());

            await act(async () => {
                hoisted.registeredHandlers!.response(createMockResponse({status: 200, url: lookupUrl}));
            });

            expect(useWorkflowIssuesStore.getState().liveIssues).toEqual({});
        });
    });
});
