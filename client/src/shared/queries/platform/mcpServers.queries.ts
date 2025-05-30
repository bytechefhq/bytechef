import {McpComponentType} from '@/shared/queries/platform/mcpComponents.queries';
import {useQuery} from '@tanstack/react-query';

export type McpServerType = {
    id: number;
    name: string;
    type: string;
    environment: string;
    enabled: boolean;
    lastModifiedDate: number;
    mcpComponents: McpComponentType[];
    tags?: {id: number}[];
};

export const McpServerKeys = {
    mcpServer: (id: number) => [...McpServerKeys.mcpServers, id],
    mcpServers: ['mcpServers'] as const,
};

export const useGetMcpServerQuery = (id: number, enabled?: boolean) =>
    useQuery<McpServerType, Error>({
        enabled: enabled === undefined ? true : enabled,
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query mcpServer($id: Int!) {
                            mcpServer(id: $id) {
                                id
                                name
                                type
                                environment
                                enabled
                            }
                        }
                    `,
                    variables: {id},
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });
            const json = await response.json();
            return json.data.mcpServer;
        },
        queryKey: McpServerKeys.mcpServer(id),
    });

export const useGetMcpServersQuery = () =>
    useQuery<McpServerType[], Error>({
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query {
                            mcpServers {
                                id
                                name
                                type
                                environment
                                enabled
                            }
                        }
                    `,
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });
            const json = await response.json();
            return json.data.mcpServers;
        },
        queryKey: McpServerKeys.mcpServers,
    });
