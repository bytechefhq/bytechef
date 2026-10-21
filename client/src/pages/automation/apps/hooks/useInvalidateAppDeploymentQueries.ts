import {useQueryClient} from '@tanstack/react-query';
import {useCallback} from 'react';

const APP_DEPLOYMENT_QUERY_KEYS = ['app', 'appDeployments', 'appInstance', 'appInstanceDeploymentSummary', 'apps'];

export default function useInvalidateAppDeploymentQueries() {
    const queryClient = useQueryClient();

    return useCallback(() => {
        for (const queryKey of APP_DEPLOYMENT_QUERY_KEYS) {
            queryClient.invalidateQueries({queryKey: [queryKey]});
        }
    }, [queryClient]);
}
