import {McpServer, useUpdateEmbeddedMcpServerUrlMutation} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

const useMcpServerList = (mcpServers: McpServer[]) => {
    const updateEmbeddedMcpServerUrlMutation = useUpdateEmbeddedMcpServerUrlMutation({});

    const queryClient = useQueryClient();

    const sortedMcpServers = useMemo(
        () =>
            [...mcpServers].sort((previousMcpServer, currentMcpServer) =>
                previousMcpServer.name.localeCompare(currentMcpServer.name)
            ),
        [mcpServers]
    );

    const createHandleRefresh = (mcpServerId: string): (() => void) => {
        return () => {
            updateEmbeddedMcpServerUrlMutation.mutate(
                {
                    id: mcpServerId,
                },
                {
                    onSuccess: () => {
                        queryClient.invalidateQueries({queryKey: ['embeddedMcpServers']});
                    },
                }
            );
        };
    };

    return {createHandleRefresh, sortedMcpServers};
};

export default useMcpServerList;
