import {useAgentExternalChangeStore} from '@/pages/automation/agents/stores/useAgentExternalChangeStore';
import {type QueryClient} from '@tanstack/react-query';

const snapshotAgentQueries = (queryClient: QueryClient) =>
    JSON.stringify(queryClient.getQueriesData({queryKey: ['aiAgent']}));

export default async function refreshAgentAfterExternalChange(queryClient: QueryClient) {
    const agentQueriesBefore = snapshotAgentQueries(queryClient);

    await Promise.all([
        queryClient.invalidateQueries({queryKey: ['aiAgent']}),
        queryClient.invalidateQueries({queryKey: ['aiAgents']}),
    ]);

    if (snapshotAgentQueries(queryClient) !== agentQueriesBefore) {
        useAgentExternalChangeStore.getState().bumpRevision();
    }
}
