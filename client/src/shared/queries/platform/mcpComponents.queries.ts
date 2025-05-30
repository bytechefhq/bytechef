import {useQuery} from '@tanstack/react-query';

export type McpComponentType = {
    id: number;
    componentName: string;
    componentVersion: number;
    mcpServerId: number;
    connectionId?: number;
};

export const McpComponentKeys = {
    mcpComponent: (id: number) => [...McpComponentKeys.mcpComponents, id],
    mcpComponents: ['mcpComponents'] as const,
    mcpComponentsByServerId: (mcpServerId: number) => [...McpComponentKeys.mcpComponents, 'byServerId', mcpServerId],
};

export const useGetMcpComponentQuery = (id: number, enabled?: boolean) =>
    useQuery<McpComponentType, Error>({
        enabled: enabled === undefined ? true : enabled,
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query mcpComponent($id: Int!) {
                            mcpComponent(id: $id) {
                                id
                                componentName
                                componentVersion
                                mcpServerId
                                connectionId
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
            return json.data.mcpComponent;
        },
        queryKey: McpComponentKeys.mcpComponent(id),
    });

export const useGetMcpComponentsQuery = () =>
    useQuery<McpComponentType[], Error>({
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query {
                            mcpComponents {
                                id
                                componentName
                                componentVersion
                                mcpServerId
                                connectionId
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
            return json.data.mcpComponents;
        },
        queryKey: McpComponentKeys.mcpComponents,
    });

export const useGetMcpComponentsByServerIdQuery = (mcpServerId: number, enabled?: boolean) =>
    useQuery<McpComponentType[], Error>({
        enabled: enabled === undefined ? true : enabled,
        queryFn: async () => {
            const response = await fetch('/graphql', {
                body: JSON.stringify({
                    query: `
                        query mcpComponentsByServerId($mcpServerId: Int!) {
                            mcpComponentsByServerId(mcpServerId: $mcpServerId) {
                                id
                                componentName
                                componentVersion
                                mcpServerId
                                connectionId
                            }
                        }
                    `,
                    variables: {mcpServerId},
                }),
                headers: {
                    'Content-Type': 'application/json',
                },
                method: 'POST',
            });
            const json = await response.json();
            return json.data.mcpComponentsByServerId;
        },
        queryKey: McpComponentKeys.mcpComponentsByServerId(mcpServerId),
    });
