import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiAgentsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const useAgents = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data,
        error: agentsError,
        isLoading: agentsIsLoading,
    } = useAiAgentsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const agents = useMemo(() => (data?.aiAgents ?? []).filter((agent) => agent != null), [data]);

    return {agents, agentsError, agentsIsLoading};
};

export default useAgents;
