import useWorkflowIssuesStore from '@/pages/platform/workflow-editor/stores/useWorkflowIssuesStore';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import useFetchInterceptor, {clearActiveToasts} from '../useFetchInterceptor';

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

            expect((result as [string, RequestInit])[1].headers).toBeInstanceOf(Headers);
            expect(((result as [string, RequestInit])[1].headers as Headers).get('X-XSRF-TOKEN')).toBe(
                'test-xsrf-token'
            );
        });

        it('adds XSRF-TOKEN header for graphql URLs', () => {
            renderHook(() => useFetchInterceptor());

            const result = hoisted.registeredHandlers!.request('/graphql', {headers: {}});

            expect((result as [string, RequestInit])[1].headers).toBeInstanceOf(Headers);
            expect(((result as [string, RequestInit])[1].headers as Headers).get('X-XSRF-TOKEN')).toBe(
                'test-xsrf-token'
            );
        });

        it('replaces, not duplicates, an XSRF token header the caller already set', () => {
            renderHook(() => useFetchInterceptor());

            const [, config] = hoisted.registeredHandlers!.request('/graphql', {
                headers: {'Content-Type': 'application/json', 'x-xsrf-token': 'stale-token'},
            }) as [string, RequestInit];

            const sentHeaders = config.headers as Headers;

            expect(sentHeaders.get('X-XSRF-TOKEN')).toBe('test-xsrf-token');
            expect(sentHeaders.get('Content-Type')).toBe('application/json');
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

        it('does not clear auth or navigate on a 403 for a csrf-protected url', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 403, url: 'http://localhost/internal/api/test'});

            const result = hoisted.registeredHandlers!.response(response);

            expect(result).toBe(response);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('does not toast on a 403 for a csrf-protected url', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 403, url: 'http://localhost/graphql'});

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('does not navigate to login for /api/account endpoint', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 401, url: 'http://localhost/api/account'});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });
    });

    describe('response interceptor - access denied', () => {
        it('toasts an access-denied 403 on a csrf-protected url without clearing auth', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {detail: 'Access denied', errorCode: 'ACCESS_DENIED', status: 403, title: 'Forbidden'},
                status: 403,
                url: 'http://localhost/api/automation/internal/workflow-executions',
            });

            const result = hoisted.registeredHandlers!.response(response);

            await waitFor(
                () => {
                    expect(hoisted.toastError).toHaveBeenCalledWith('Forbidden', {
                        description: 'Access denied',
                        id: 'fetch-error-403',
                        onAutoClose: expect.any(Function),
                        onDismiss: expect.any(Function),
                    });
                },
                {interval: 5, timeout: 5000}
            );

            expect(result).toBe(response);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });
    });

    describe('response interceptor - trial expiration', () => {
        afterEach(() => {
            window.history.pushState({}, '', '/');
        });

        it('navigates to billing settings on 402', () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 402, url: 'http://localhost/internal/api/test'});

            hoisted.registeredHandlers!.response(response);

            expect(hoisted.navigate).toHaveBeenCalledWith('/automation/settings/billing');
        });

        it('does not navigate again when already on the billing settings page', () => {
            window.history.pushState({}, '', '/automation/settings/billing');

            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({status: 402, url: 'http://localhost/internal/api/test'});

            hoisted.registeredHandlers!.response(response);

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

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', {
                description: 'Something went wrong',
                id: 'fetch-error-500',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
            });
        });

        it('skips toast for approval-form read errors handled inline', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {detail: 'Approval form is no longer available.', title: 'Not Found'},
                status: 404,
                url: 'http://localhost/internal/approval-form/abc123',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('skips toast for AI Hub chat turn errors, which the hub surfaces itself', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {error: 'TURN_IN_FLIGHT'},
                status: 409,
                url: 'http://localhost/api/platform/internal/ai/chat/ai_hub',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('still toasts errors from other agents chat endpoints and other internal urls', async () => {
            renderHook(() => useFetchInterceptor());

            for (const url of [
                'http://localhost/api/platform/internal/ai/chat/workflow_editor_build',
                'http://localhost/api/platform/internal/ai/chat/ai_hub_other',
                'http://localhost/api/platform/internal/projects/1',
            ]) {
                clearActiveToasts();

                hoisted.registeredHandlers!.response(
                    createMockResponse({jsonData: {detail: 'Conflict', title: 'Error'}, status: 409, url})
                );

                await act(async () => {
                    await new Promise((resolve) => setTimeout(resolve, 0));
                });
            }

            expect(hoisted.toastError).toHaveBeenCalledTimes(3);
        });

        it('skips toast for a failed AI Hub presence heartbeat', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {detail: 'Unknown thread', title: 'Not Found'},
                status: 404,
                url: 'http://localhost/api/platform/internal/ai/chat/ai_hub/thread-1/presence',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('skips toast for a failed AI Hub status poll', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {detail: 'Something went wrong', title: 'Error'},
                status: 500,
                url: 'http://localhost/api/platform/internal/ai/chat/ai_hub/status?threadIds=thread-1',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
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

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', {
                description: 'Field not found\nPermission denied',
                id: 'fetch-error-200',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
            });
        });

        it('does not toast a CONFLICT on resolving an AI Hub tool approval, which the approval card shows', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {
                    errors: [
                        {
                            extensions: {errorCode: 'CONFLICT'},
                            message: 'Approval 4 is already APPROVED',
                            path: ['resolveAiHubToolApproval'],
                        },
                    ],
                },
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('still toasts another error on resolving an AI Hub tool approval', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {
                    errors: [
                        {
                            extensions: {errorCode: 'NOT_FOUND'},
                            message: 'Not found',
                            path: ['resolveAiHubToolApproval'],
                        },
                    ],
                },
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledWith(
                'Error',
                expect.objectContaining({description: 'Not found'})
            );
        });

        it('still toasts a CONFLICT from another GraphQL field', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {
                    errors: [
                        {
                            extensions: {errorCode: 'CONFLICT'},
                            message: 'AiHubChat thread-1 already exists',
                            path: ['createAiHubChat'],
                        },
                    ],
                },
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledWith(
                'Error',
                expect.objectContaining({description: 'AiHubChat thread-1 already exists'})
            );
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

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('clears the session instead of toasting an unauthenticated GraphQL error', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {
                    errors: [{extensions: {errorCode: 'AUTHENTICATION_REQUIRED'}, message: 'Authentication required'}],
                },
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();
            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).toHaveBeenCalled();
        });

        it('still toasts GraphQL errors carrying an unrelated error code', async () => {
            renderHook(() => useFetchInterceptor());

            const response = createMockResponse({
                jsonData: {errors: [{extensions: {errorCode: 'ACCESS_DENIED'}, message: 'Access denied'}]},
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(response);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', {
                description: 'Access denied',
                id: 'fetch-error-200',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
            });
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
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

            expect(hoisted.toastError).not.toHaveBeenCalled();
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

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', {
                description: 'Unknown error\nValid error',
                id: 'fetch-error-200',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
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

            expect(hoisted.toastError).toHaveBeenCalledWith('Request failed with status 500', {
                id: 'fetch-error-500',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
            });
        });

        it('does not suppress GraphQL error toast after successful GraphQL 200', async () => {
            renderHook(() => useFetchInterceptor());

            const successResponse = createMockResponse({
                jsonData: {data: {users: []}},
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(successResponse);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).not.toHaveBeenCalled();

            const errorResponse = createMockResponse({
                jsonData: {errors: [{message: 'Something broke'}]},
                status: 200,
                url: 'http://localhost/graphql',
            });

            hoisted.registeredHandlers!.response(errorResponse);

            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

            expect(hoisted.toastError).toHaveBeenCalledWith('Error', {
                description: 'Something broke',
                id: 'fetch-error-200',
                onAutoClose: expect.any(Function),
                onDismiss: expect.any(Function),
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

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });
    });

    describe('response interceptor - node test output errors', () => {
        const flush = () =>
            act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 0));
            });

        it('skips the toast for a failed node test, which the Output tab renders inline', async () => {
            renderHook(() => useFetchInterceptor());

            hoisted.registeredHandlers!.response(
                createMockResponse({
                    jsonData: {detail: 'All scraping engines failed', title: 'Error'},
                    status: 500,
                    url: 'http://localhost/internal/workflows/wf-1/workflow-nodes/firecrawl_1/test-outputs?environmentId=0',
                })
            );

            await flush();

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('still toasts a failed sample output upload', async () => {
            renderHook(() => useFetchInterceptor());

            hoisted.registeredHandlers!.response(
                createMockResponse({
                    jsonData: {detail: 'Invalid sample output', title: 'Bad Request'},
                    status: 400,
                    url: 'http://localhost/internal/workflows/wf-1/workflow-nodes/firecrawl_1/test-outputs/sample-output',
                })
            );

            await flush();

            expect(hoisted.toastError).toHaveBeenCalledWith(
                'Bad Request',
                expect.objectContaining({description: 'Invalid sample output'})
            );
        });

        it('skips the toast when every GraphQL error comes from the cluster element test', async () => {
            renderHook(() => useFetchInterceptor());

            hoisted.registeredHandlers!.response(
                createMockResponse({
                    jsonData: {errors: [{message: 'Tool failed', path: ['saveClusterElementTestOutput']}]},
                    status: 200,
                    url: 'http://localhost/graphql',
                })
            );

            await flush();

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });

        it('still toasts when a GraphQL response also carries an error from another field', async () => {
            renderHook(() => useFetchInterceptor());

            hoisted.registeredHandlers!.response(
                createMockResponse({
                    jsonData: {
                        errors: [
                            {message: 'Tool failed', path: ['saveClusterElementTestOutput']},
                            {message: 'Workflow not found', path: ['workflow']},
                        ],
                    },
                    status: 200,
                    url: 'http://localhost/graphql',
                })
            );

            await flush();

            expect(hoisted.toastError).toHaveBeenCalledWith(
                'Error',
                expect.objectContaining({description: 'Tool failed\nWorkflow not found'})
            );
        });

        it('skips the toast for a failed AI Hub chat title, which the chats sidebar marks', async () => {
            renderHook(() => useFetchInterceptor());

            hoisted.registeredHandlers!.response(
                createMockResponse({
                    jsonData: {
                        errors: [
                            {
                                extensions: {errorCode: 'TITLE_GENERATION_FAILED'},
                                message: 'Chat title generation is temporarily unavailable.',
                                path: ['generateAiHubChatTitle'],
                            },
                        ],
                    },
                    status: 200,
                    url: 'http://localhost/graphql',
                })
            );

            await flush();

            expect(hoisted.toastError).not.toHaveBeenCalled();
        });
    });

    describe('csrf-aware fetch wrapper', () => {
        let originalFetch: typeof window.fetch;

        beforeEach(() => {
            originalFetch = window.fetch;
        });

        afterEach(() => {
            window.fetch = originalFetch;
        });

        it('refreshes the token and replays a graphql 403 once, returning the recovered response', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/graphql'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/graphql'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/graphql', {method: 'POST'});

            expect((result as Response).status).toBe(200);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(innerFetch).toHaveBeenNthCalledWith(2, '/api/account', {method: 'GET'});
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('retries an internal REST 403 the same way', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/internal/api/test'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/internal/api/test'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/internal/api/test', {method: 'POST'});

            expect((result as Response).status).toBe(200);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('escalates to logout when the replay also returns 403', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/graphql'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/graphql'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/graphql', {method: 'POST'});

            expect((result as Response).status).toBe(403);
            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).toHaveBeenCalled();
            expect(hoisted.navigate).toHaveBeenCalledWith('/login');
        });

        it('does not retry or escalate a 403 on a non-csrf-protected url', async () => {
            const innerFetch = vi.fn().mockResolvedValueOnce({status: 403, url: 'http://localhost/api/public/test'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/public/test', {});

            expect((result as Response).status).toBe(403);
            expect(innerFetch).toHaveBeenCalledTimes(1);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('does not retry or log out when the AI Hub chat turn endpoint refuses the turn itself', async () => {
            const innerFetch = vi.fn().mockResolvedValueOnce(
                createMockResponse({
                    jsonData: {error: 'Workspace is not accessible to the current user'},
                    status: 403,
                    url: 'http://localhost/api/platform/internal/ai/chat/ai_hub',
                })
            );

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/platform/internal/ai/chat/ai_hub', {method: 'POST'});

            expect((result as Response).status).toBe(403);
            expect(innerFetch).toHaveBeenCalledTimes(1);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('reads the AI Hub refusal from a clone, leaving the body for the caller', async () => {
            const refusalResponse = new Response(
                JSON.stringify({error: 'AiHubChat is not accessible to the current user'}),
                {
                    headers: {'Content-Type': 'application/json'},
                    status: 403,
                }
            );

            const innerFetch = vi.fn().mockResolvedValueOnce(refusalResponse);

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/platform/internal/ai/chat/ai_hub', {method: 'POST'});

            expect(innerFetch).toHaveBeenCalledTimes(1);
            expect(result.bodyUsed).toBe(false);
            await expect(result.json()).resolves.toEqual({error: 'AiHubChat is not accessible to the current user'});
        });

        it('replays a CSRF 403 without a JSON body from the AI Hub chat turn endpoint', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce(
                    createMockResponse({
                        jsonRejects: true,
                        status: 403,
                        url: 'http://localhost/api/platform/internal/ai/chat/ai_hub',
                    })
                )
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/platform/internal/ai/chat/ai_hub'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/platform/internal/ai/chat/ai_hub', {method: 'POST'});

            expect((result as Response).status).toBe(200);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(innerFetch).toHaveBeenNthCalledWith(2, '/api/account', {method: 'GET'});
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('does not log out when the replay of a CSRF 403 is the AI Hub controller refusing the turn', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce(
                    createMockResponse({
                        jsonRejects: true,
                        status: 403,
                        url: 'http://localhost/api/platform/internal/ai/chat/ai_hub',
                    })
                )
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce(
                    createMockResponse({
                        jsonData: {error: 'Workspace is not accessible to the current user'},
                        status: 403,
                        url: 'http://localhost/api/platform/internal/ai/chat/ai_hub',
                    })
                );

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/platform/internal/ai/chat/ai_hub', {method: 'POST'});

            expect((result as Response).status).toBe(403);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('replays a 403 from the AI Hub chat turn endpoint whose JSON body is not the controller refusal', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce(
                    createMockResponse({
                        jsonData: {
                            error: 'Forbidden',
                            path: '/api/platform/internal/ai/chat/ai_hub',
                            status: 403,
                            timestamp: '2026-10-19T12:00:00.000+00:00',
                        },
                        status: 403,
                        url: 'http://localhost/api/platform/internal/ai/chat/ai_hub',
                    })
                )
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/api/platform/internal/ai/chat/ai_hub'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/platform/internal/ai/chat/ai_hub', {method: 'POST'});

            expect((result as Response).status).toBe(403);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).toHaveBeenCalled();
            expect(hoisted.navigate).toHaveBeenCalledWith('/login');
        });

        it('still replays and escalates a 403 from another agent chat endpoint', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/api/platform/internal/ai/chat/copilot'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/api/platform/internal/ai/chat/copilot'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/platform/internal/ai/chat/copilot', {method: 'POST'});

            expect((result as Response).status).toBe(403);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(hoisted.clearAuthentication).toHaveBeenCalled();
            expect(hoisted.navigate).toHaveBeenCalledWith('/login');
        });

        it('does not retry a non-403 response', async () => {
            const innerFetch = vi.fn().mockResolvedValueOnce({status: 200, url: 'http://localhost/graphql'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/graphql', {method: 'POST'});

            expect((result as Response).status).toBe(200);
            expect(innerFetch).toHaveBeenCalledTimes(1);
        });
    });

    describe('csrf-aware fetch wrapper - access denied', () => {
        let originalFetch: typeof window.fetch;

        beforeEach(() => {
            originalFetch = window.fetch;
        });

        afterEach(() => {
            window.fetch = originalFetch;
        });

        it('neither replays nor logs out on an access-denied 403', async () => {
            const accessDeniedResponse = createMockResponse({
                jsonData: {detail: 'Access denied', errorCode: 'ACCESS_DENIED', status: 403, title: 'Forbidden'},
                status: 403,
                url: 'http://localhost/api/automation/internal/workflow-executions',
            });

            const innerFetch = vi.fn().mockResolvedValue(accessDeniedResponse);

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/api/automation/internal/workflow-executions', {method: 'GET'});

            expect(result).toBe(accessDeniedResponse);
            expect(innerFetch).toHaveBeenCalledTimes(1);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
        });

        it('still replays a csrf 403 whose body is not an access-denied problem detail', async () => {
            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce(
                    createMockResponse({jsonRejects: true, status: 403, url: 'http://localhost/internal/api/test'})
                )
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/internal/api/test'});

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/internal/api/test', {method: 'POST'});

            expect((result as Response).status).toBe(200);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
        });

        it('does not log out when the replay of a csrf 403 is an access-denied 403', async () => {
            const accessDeniedResponse = createMockResponse({
                jsonData: {detail: 'Access denied', errorCode: 'ACCESS_DENIED', status: 403, title: 'Forbidden'},
                status: 403,
                url: 'http://localhost/internal/api/test',
            });

            const innerFetch = vi
                .fn()
                .mockResolvedValueOnce({status: 403, url: 'http://localhost/internal/api/test'})
                .mockResolvedValueOnce({status: 200, url: 'http://localhost/api/account'})
                .mockResolvedValueOnce(accessDeniedResponse);

            window.fetch = innerFetch as unknown as typeof window.fetch;

            renderHook(() => useFetchInterceptor());

            const result = await window.fetch('/internal/api/test', {method: 'POST'});

            expect(result).toBe(accessDeniedResponse);
            expect(innerFetch).toHaveBeenCalledTimes(3);
            expect(hoisted.clearAuthentication).not.toHaveBeenCalled();
            expect(hoisted.clearCurrentWorkspaceId).not.toHaveBeenCalled();
            expect(hoisted.navigate).not.toHaveBeenCalled();
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
