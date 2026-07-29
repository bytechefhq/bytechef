import {PlatformType, useA2aServersQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

const useA2aServers = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {
        data,
        error: a2aServersError,
        isLoading: a2aServersIsLoading,
    } = useA2aServersQuery({
        type: PlatformType.Automation,
    });

    const a2aServers = useMemo(
        () =>
            (data?.a2aServers ?? []).filter(
                (a2aServer) => a2aServer != null && Number(a2aServer.environmentId) === currentEnvironmentId
            ),
        [data, currentEnvironmentId]
    );

    return {a2aServers, a2aServersError, a2aServersIsLoading};
};

export default useA2aServers;
