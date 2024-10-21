import {useToast} from '@/hooks/use-toast';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {getCookie} from '@/shared/util/cookie-utils';
import fetchIntercept from 'fetch-intercept';
import {useNavigate} from 'react-router-dom';

export default function useFetchInterceptor() {
    const {clearAuthentication} = useAuthenticationStore();
    const {clearCurrentWorkspaceId} = useWorkspaceStore();

    const navigate = useNavigate();
    const {toast} = useToast();

    const unregister = fetchIntercept.register({
        /* eslint-disable-next-line @typescript-eslint/no-explicit-any */
        request(url: string, config: any): Promise<any[]> | any[] {
            if (url.includes('/internal/')) {
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

        response: function (response) {
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
                    .then((data) => {
                        if (data.entityClass === 'User' && data.errorKey === 100) {
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
