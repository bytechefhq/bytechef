import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {getCookie} from '@/shared/util/cookie-utils';
import fetchIntercept from 'fetch-intercept';
import {useEffect, useRef} from 'react';
import {useNavigate} from 'react-router-dom';

export default function useFetchInterceptor() {
    const clearAuthentication = useAuthenticationStore((state) => state.clearAuthentication);
    const clearCurrentWorkspaceId = useWorkspaceStore((state) => state.clearCurrentWorkspaceId);

    const navigate = useNavigate();
    const {toast} = useToast();

    const apiBasePath = import.meta.env.VITE_API_BASE_PATH;

    const latestCallbacks = {apiBasePath, clearAuthentication, clearCurrentWorkspaceId, navigate, toast};
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
                const {clearAuthentication, clearCurrentWorkspaceId, navigate, toast} = callbacksRef.current;

                if (response.status === 403 || response.status === 401) {
                    clearAuthentication();
                    clearCurrentWorkspaceId();

                    if (!response.url.endsWith('/api/account')) {
                        navigate('/login');
                    }

                    return response;
                }

                if (response.url.includes('/graphql')) {
                    const clonedResponse = response.clone();

                    clonedResponse
                        .json()
                        .then((data: {errors?: Array<{message?: string}>}) => {
                            if (data.errors?.length) {
                                const errorMessage = data.errors
                                    .map((error) => error.message || 'Unknown error')
                                    .join('\n');

                                toast({
                                    description: errorMessage,
                                    title: 'GraphQL Error',
                                    variant: 'destructive',
                                });
                            } else if (response.status < 200 || response.status > 299) {
                                toast({
                                    description: `Request failed with status ${response.status}`,
                                    variant: 'destructive',
                                });
                            }
                        })
                        .catch(() => {
                            if (response.status < 200 || response.status > 299) {
                                toast({
                                    description: `Request failed with status ${response.status}`,
                                    variant: 'destructive',
                                });
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

                            toast({
                                description: data.detail,
                                title: data.title,
                                variant: 'destructive',
                            });
                        })
                        .catch(() => {
                            toast({
                                description: `Request failed with status ${response.status}`,
                                variant: 'destructive',
                            });
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
