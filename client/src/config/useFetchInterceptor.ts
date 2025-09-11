import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';

export default function useFetchInterceptor() {
    const clearAuthentication = useAuthenticationStore((state) => state.clearAuthentication);
    const {clearCurrentWorkspaceId} = useWorkspaceStore();
    const [fetchIntercept, setFetchIntercept] = useState<typeof import('fetch-intercept') | null>(null);

    const navigate = useNavigate();
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

            if (url.includes('/internal/') || url.includes('graphql')) {
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
            if (response.status === 403 || response.status === 401) {
                clearAuthentication();
                clearCurrentWorkspaceId();

                if (!response.url.endsWith('/api/account')) {
                    navigate('/login');
                }

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
