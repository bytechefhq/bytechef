import {useMcpComponentsByServerIdQuery} from '@/shared/middleware/graphql';

export default function useMcpComponentList(mcpServerId: string) {
    const {data, isLoading: isMcpComponentsLoading} = useMcpComponentsByServerIdQuery({
        mcpServerId,
    });

    return {
        data,
        isMcpComponentsLoading,
    };
}
