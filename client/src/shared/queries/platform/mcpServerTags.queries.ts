import {useQuery} from '@tanstack/react-query';

export type McpServerTagType = {
    id: number;
    name: string;
};

export const McpServerTagKeys = {
    mcpServers: ['mcpServerTags'] as const,
};

export const useGetMcpServerTagsQuery = (type?: string) =>
    useQuery<McpServerTagType[], Error>({
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query mcpServerTags($type: String) {
                            mcpServerTags(type: $type) {
                                id
                                name
                            }
                        }
                    `,
                    variables: {
                        type,
                    },
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });
            const json = await response.json();
            return json.data.tags;
        },
        queryKey: [...McpServerTagKeys.mcpServers, type],
    });
