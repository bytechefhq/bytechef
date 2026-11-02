import {type QueryClient} from '@tanstack/react-query';

export default function invalidateAgentQueries(queryClient: QueryClient) {
    queryClient.invalidateQueries({queryKey: ['aiAgent']});
    queryClient.invalidateQueries({queryKey: ['aiAgents']});
}
