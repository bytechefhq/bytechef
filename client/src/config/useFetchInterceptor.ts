import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {getCookie} from '@/shared/util/cookie-utils';
import fetchIntercept from 'fetch-intercept';
import {useEffect, useRef} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

const activeToastIds = new Set<string>();

export function clearActiveToasts() {
    activeToastIds.clear();
}

const TOAST_SAFETY_TIMEOUT_MS = 30000;

/*
 * A 403 on a CSRF-protected endpoint (`/graphql`, `/internal/`) is almost always a transient CSRF
 * token race rather than a real authorization failure — genuine auth failures return 401 here. The
 * token is rotated whenever the session silently re-authenticates (e.g. remember-me kicking in after
 * a session timeout), or the XSRF-TOKEN cookie has not been established yet, so a request already in
 * flight can carry a stale/empty token and be rejected. We refresh the token and replay the request
 * once before treating it as fatal. See https://github.com/bytechefhq/bytechef/issues/5189.
 */
function isCsrfProtectedUrl(url: string): boolean {
    return url.includes('/graphql') || url.includes('/internal/');
}

function resolveUrl(input: RequestInfo | URL): string {
    if (typeof input === 'string') {
        return input;
    }

    if (input instanceof URL) {
        return input.toString();
    }

    return input.url;
}

let csrfRefreshPromise: Promise<void> | null = null;

/*
 * Coalesce concurrent refreshes so a page that fires a batch of requests in parallel (e.g. the
 * executions page) triggers a single `/api/account` round-trip rather than one per failed request. A
 * plain GET re-establishes the XSRF-TOKEN cookie via the server's CookieCsrfFilter.
 */
function refreshCsrfToken(nativeFetch: typeof fetch, apiBasePath?: string): Promise<void> {
    if (!csrfRefreshPromise) {
        csrfRefreshPromise = nativeFetch((apiBasePath || '') + '/api/account', {method: 'GET'})
            .then(() => undefined)
            .catch(() => undefined)
            .finally(() => {
                csrfRefreshPromise = null;
            });
    }

    return csrfRefreshPromise;
}

function showErrorToast(toastId: string, title: string, options?: {description?: string}) {
    if (activeToastIds.has(toastId)) {
        return;
    }

    activeToastIds.add(toastId);

    setTimeout(() => activeToastIds.delete(toastId), TOAST_SAFETY_TIMEOUT_MS);

    toast.error(title, {
        ...options,
        id: toastId,
        onAutoClose: () => activeToastIds.delete(toastId),
        onDismiss: () => activeToastIds.delete(toastId),
    });
}

export default function useFetchInterceptor() {
    const clearAuthentication = useAuthenticationStore((state) => state.clearAuthentication);
    const clearCurrentWorkspaceId = useWorkspaceStore((state) => state.clearCurrentWorkspaceId);

    const navigate = useNavigate();

    const apiBasePath = import.meta.env.VITE_API_BASE_PATH;

    const latestCallbacks = {apiBasePath, clearAuthentication, clearCurrentWorkspaceId, navigate};
    const callbacksRef = useRef(latestCallbacks);

    callbacksRef.current = latestCallbacks;

    useEffect(() => {
        const nativeFetch = window.fetch;

        const unregister = fetchIntercept.register({
            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
            request(url: string, config: any): Promise<any[]> | any[] {
                const {apiBasePath} = callbacksRef.current;

                if (apiBasePath && !url.startsWith(apiBasePath)) {
                    url = apiBasePath + url;
                }

                if (url.includes('/internal/') || url.includes('/graphql')) {
                    return [
                        url,
                        {
                            ...config,
                            headers: {
                                ...config.headers,
                                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                            },
                        },
                    ];
                } else {
                    return [url, config];
                }
            },

            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
            response: function (response: any) {
                const {clearAuthentication, clearCurrentWorkspaceId, navigate} = callbacksRef.current;

                if (response.status === 401) {
                    clearAuthentication();
                    clearCurrentWorkspaceId();

                    if (!response.url.endsWith('/api/account')) {
                        navigate('/login');
                    }

                    return response;
                }

                // A 403 on a CSRF-protected endpoint is a transient token race handled by the
                // csrf-aware fetch wrapper below (refresh + replay, then escalate). Don't toast or
                // log out here, otherwise the recovered request would still flash an error.
                if (response.status === 403 && isCsrfProtectedUrl(response.url)) {
                    return response;
                }

                const toastId = `fetch-error-${response.status}`;

                if (response.url.includes('/graphql')) {
                    const clonedResponse = response.clone();

                    clonedResponse
                        .json()
                        .then((data: {errors?: Array<{message?: string}>}) => {
                            if (data.errors?.length) {
                                const errorMessage = [
                                    ...new Set(data.errors.map((error) => error.message || 'Unknown error')),
                                ].join('\n');

                                showErrorToast(toastId, 'Error', {description: errorMessage});
                            } else if (response.status < 200 || response.status > 299) {
                                showErrorToast(toastId, `Request failed with status ${response.status}`);
                            }
                        })
                        .catch(() => {
                            if (response.status < 200 || response.status > 299) {
                                showErrorToast(toastId, `Request failed with status ${response.status}`);
                            }
                        });
                } else if (response.status < 200 || response.status > 299) {
                    const clonedResponse = response.clone();

                    clonedResponse
                        .json()
                        .then((data: {entityClass?: string; errorKey?: number; detail?: string; title?: string}) => {
                            if (data.entityClass === 'AdminUserDTO' && data.errorKey === 100) {
                                return;
                            }

                            showErrorToast(toastId, data.title || 'Error', {description: data.detail});
                        })
                        .catch(() => {
                            showErrorToast(toastId, `Request failed with status ${response.status}`);
                        });
                }

                return response;
            },
        });

        const interceptedFetch = window.fetch;

        const csrfAwareFetch: typeof window.fetch = async (input, init) => {
            const response = await interceptedFetch(input, init);

            // A Request body may be a one-shot stream, so only replay calls made with a plain
            // url + init (which is how every GraphQL and REST call is issued here).
            if (response.status !== 403 || input instanceof Request) {
                return response;
            }

            const url = resolveUrl(input);

            if (!isCsrfProtectedUrl(url)) {
                return response;
            }

            const {apiBasePath, clearAuthentication, clearCurrentWorkspaceId, navigate} = callbacksRef.current;

            await refreshCsrfToken(nativeFetch, apiBasePath);

            const retriedResponse = await interceptedFetch(input, init);

            if (retriedResponse.status === 403) {
                clearAuthentication();
                clearCurrentWorkspaceId();

                if (!url.endsWith('/api/account')) {
                    navigate('/login');
                }
            }

            return retriedResponse;
        };

        window.fetch = csrfAwareFetch;

        return () => {
            unregister();

            window.fetch = nativeFetch;
        };
    }, []);
}
