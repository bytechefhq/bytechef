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

                if (response.status === 403 || response.status === 401) {
                    clearAuthentication();
                    clearCurrentWorkspaceId();

                    if (!response.url.endsWith('/api/account')) {
                        navigate('/login');
                    }

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

        return () => {
            unregister();
        };
    }, []);
}
