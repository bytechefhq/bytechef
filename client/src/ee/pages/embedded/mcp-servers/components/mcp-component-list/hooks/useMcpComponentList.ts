import {useEmbeddedMcpComponentsByServerIdQuery} from '@/shared/middleware/graphql';

export default function useMcpComponentList(mcpServerId: string) {
    const {data, isLoading: isMcpComponentsLoading} = useEmbeddedMcpComponentsByServerIdQuery({
        mcpServerId,
    });

    return {
        data,
        isMcpComponentsLoading,
    };
}
