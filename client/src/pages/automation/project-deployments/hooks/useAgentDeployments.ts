import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useAiAgentDeploymentsQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

const useAgentDeployments = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {
        data,
        error: agentDeploymentsError,
        isLoading: agentDeploymentsIsLoading,
    } = useAiAgentDeploymentsQuery({
        workspaceId: currentWorkspaceId + '',
    });

    const agentDeployments = useMemo(
        () => (data?.aiAgentDeployments ?? []).filter((agentDeployment) => agentDeployment != null),
        [data]
    );

    return {agentDeployments, agentDeploymentsError, agentDeploymentsIsLoading};
};

export default useAgentDeployments;
