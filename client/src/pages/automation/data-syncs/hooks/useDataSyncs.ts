import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useDataSyncsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const useDataSyncs = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data,
        error: dataSyncsError,
        isLoading: dataSyncsIsLoading,
    } = useDataSyncsQuery({workspaceId: currentWorkspaceId + ''});

    const dataSyncs = useMemo(() => (data?.dataSyncs ?? []).filter((dataSync) => dataSync != null), [data]);

    return {dataSyncs, dataSyncsError, dataSyncsIsLoading};
};

export default useDataSyncs;
