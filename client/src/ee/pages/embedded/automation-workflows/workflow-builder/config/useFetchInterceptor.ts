import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useState} from 'react';

export default function useFetchInterceptor() {
    const clearAuthentication = useAuthenticationStore((state) => state.clearAuthentication);
    const clearCurrentEnvironmentId = useEnvironmentStore((state) => state.clearCurrentEnvironmentId);
    const clearCurrentWorkspaceId = useWorkspaceStore((state) => state.clearCurrentWorkspaceId);
    const [fetchIntercept, setFetchIntercept] = useState<typeof import('fetch-intercept') | null>(null);

    const {toast} = useToast();

    const apiBasePath = import.meta.env.VITE_API_BASE_PATH;

    useEffect(() => {
        import('fetch-intercept').then((module) => setFetchIntercept(module));
    }, []);

    if (!fetchIntercept) {
        return {unregister: () => {}};
    }

    const unregister = fetchIntercept.register({
        /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
        request(url: string, config: any): Promise<any[]> | any[] {
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
                            // 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
                        },
                    },
                ];
            } else {
                return [url, config];
            }
        },

        /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
        response: function (response: any) {
            if (response.status === 403 || response.status === 401) {
                clearAuthentication();
                clearCurrentEnvironmentId();
                clearCurrentWorkspaceId();

                return response;
            }

            if (response.status < 200 || response.status > 299) {
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
                    .catch(() => {});
            }

            return response;
        },
    });

    return {unregister};
}
