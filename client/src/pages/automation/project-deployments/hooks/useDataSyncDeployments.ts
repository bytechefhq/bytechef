import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useDataSyncDeploymentsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const useDataSyncDeployments = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data,
        error: dataSyncDeploymentsError,
        isLoading: dataSyncDeploymentsIsLoading,
    } = useDataSyncDeploymentsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const dataSyncDeployments = useMemo(
        () => (data?.dataSyncDeployments ?? []).filter((dataSyncDeployment) => dataSyncDeployment != null),
        [data]
    );

    return {dataSyncDeployments, dataSyncDeploymentsError, dataSyncDeploymentsIsLoading};
};

export default useDataSyncDeployments;
