import {Type} from '@/ee/pages/embedded/mcp-servers/McpServers';
import useMcpServers from '@/ee/pages/embedded/mcp-servers/hooks/useMcpServers';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    tagsData: undefined as {embeddedMcpServerTags: Array<{id: string; name: string}>} | undefined,
    tagsError: null as Error | null,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEmbeddedMcpServerTagsQuery: () => ({data: hoisted.tagsData, error: hoisted.tagsError, isLoading: false}),
    useEmbeddedMcpServersQuery: () => ({
        data: {
            embeddedMcpServers: [
                {
                    environmentId: '1',
                    id: '1',
                    mcpComponents: [{componentName: 'httpClient'}],
                    name: 'Sales',
                    tags: [{id: '10', name: 'crm'}],
                },
                {
                    environmentId: '1',
                    id: '2',
                    mcpComponents: [{componentName: 'slack'}],
                    name: 'Support',
                    tags: [{id: '11', name: 'support'}],
                },
                {environmentId: '2', id: '3', mcpComponents: [], name: 'Staging', tags: []},
                null,
            ],
        },
        error: null,
        isLoading: false,
    }),
    useMcpIntegrationInstanceConfigurationsQuery: () => ({
        data: {
            mcpIntegrationInstanceConfigurations: [{integration: {id: '3', name: 'Affinity'}, mcpServerId: '2'}],
        },
    }),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

const renderUseMcpServers = (url = '/embedded/mcp-servers') =>
    renderHook(() => useMcpServers(), {
        wrapper: ({children}: {children: ReactNode}) => <MemoryRouter initialEntries={[url]}>{children}</MemoryRouter>,
    });

const serverNames = (servers: Array<{name: string}>) => servers.map((server) => server.name);

describe('useMcpServers', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: 1});

        hoisted.tagsData = {
            embeddedMcpServerTags: [
                {id: '10', name: 'crm'},
                {id: '11', name: 'support'},
            ],
        };
        hoisted.tagsError = null;
    });

    it('returns the embedded MCP server tags', () => {
        const {result} = renderUseMcpServers();

        expect(result.current.tags).toEqual([
            {id: '10', name: 'crm'},
            {id: '11', name: 'support'},
        ]);
    });

    it('exposes the embedded tags query error', () => {
        const tagsError = new Error('tags failed');

        hoisted.tagsData = undefined;
        hoisted.tagsError = tagsError;

        const {result} = renderUseMcpServers();

        expect(result.current.tags).toBeUndefined();
        expect(result.current.tagsError).toBe(tagsError);
    });

    it('shows only the servers of the current environment when no filter is selected', () => {
        const {result} = renderUseMcpServers();

        expect(serverNames(result.current.filteredMcpServers)).toEqual(['Sales', 'Support']);
        expect(result.current.filterData).toEqual({id: undefined, type: Type.Component});
        expect(result.current.allComponentNames).toEqual(['httpClient', 'slack']);
        expect(result.current.uniqueIntegrations).toEqual([{id: '3', name: 'Affinity'}]);
    });

    it('filters the servers by the selected tag', () => {
        const {result} = renderUseMcpServers('/embedded/mcp-servers?tagId=11');

        expect(serverNames(result.current.filteredMcpServers)).toEqual(['Support']);
        expect(result.current.filterData).toEqual({id: '11', type: Type.Tag});
    });

    it('filters the servers by the selected component', () => {
        const {result} = renderUseMcpServers('/embedded/mcp-servers?componentName=httpClient');

        expect(serverNames(result.current.filteredMcpServers)).toEqual(['Sales']);
        expect(result.current.filterData).toEqual({id: 'httpClient', type: Type.Component});
    });

    it('filters the servers by the selected integration', () => {
        const {result} = renderUseMcpServers('/embedded/mcp-servers?integrationId=3');

        expect(serverNames(result.current.filteredMcpServers)).toEqual(['Support']);
        expect(result.current.filterData).toEqual({id: '3', type: Type.Integration});
    });
});
