import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import fetchIntercept from 'fetch-intercept';
import {useEffect, useRef} from 'react';
import {toast} from 'sonner';

const TOAST_COOLDOWN_MS = 10_000;
const recentToastIds = new Map<string, number>();

export function clearRecentToasts() {
    recentToastIds.clear();
}

function showErrorToast(toastId: string, title: string, options?: {description?: string}) {
    const now = Date.now();

    for (const [id, timestamp] of recentToastIds) {
        if (now - timestamp >= TOAST_COOLDOWN_MS) {
            recentToastIds.delete(id);
        }
    }

    const lastShown = recentToastIds.get(toastId);

    if (lastShown !== undefined && now - lastShown < TOAST_COOLDOWN_MS) {
        return;
    }

    recentToastIds.set(toastId, now);

    toast.error(title, {...options, id: toastId});
}

export default function useFetchInterceptor() {
    const clearAuthentication = useAuthenticationStore((state) => state.clearAuthentication);
    const clearCurrentEnvironmentId = useEnvironmentStore((state) => state.clearCurrentEnvironmentId);
    const clearCurrentWorkspaceId = useWorkspaceStore((state) => state.clearCurrentWorkspaceId);

    const apiBasePath = import.meta.env.VITE_API_BASE_PATH;

    const latestCallbacks = {
        apiBasePath,
        clearAuthentication,
        clearCurrentEnvironmentId,
        clearCurrentWorkspaceId,
    };
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

                if (url.includes('/internal/')) {
                    return [
                        url,
                        {
                            ...config,
                            headers: {
                                ...config.headers,
                                Authorization: `Bearer ${sessionStorage.getItem('jwtToken') || ''}`,
                                'X-ENVIRONMENT': sessionStorage.getItem('environment')?.toUpperCase() || '',
                            },
                        },
                    ];
                } else {
                    return [url, config];
                }
            },

            /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
            response: function (response: any) {
                const {clearAuthentication, clearCurrentEnvironmentId, clearCurrentWorkspaceId} = callbacksRef.current;

                if (response.status === 403 || response.status === 401) {
                    clearAuthentication();
                    clearCurrentEnvironmentId();
                    clearCurrentWorkspaceId();

                    return response;
                }

                const toastId = `${new URL(response.url).pathname}-${response.status}`;

                if (response.status < 200 || response.status > 299) {
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
