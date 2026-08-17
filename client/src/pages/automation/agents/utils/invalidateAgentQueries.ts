import {type QueryClient} from '@tanstack/react-query';

/* Invalidates every agent-related query (the single-agent detail view and the workspace list) so
   both refetch after a channel/element mutation. Modeled on invalidateSkillQueries. */
export default function invalidateAgentQueries(queryClient: QueryClient) {
    queryClient.invalidateQueries({queryKey: ['aiAgent']});
    queryClient.invalidateQueries({queryKey: ['aiAgents']});
}
