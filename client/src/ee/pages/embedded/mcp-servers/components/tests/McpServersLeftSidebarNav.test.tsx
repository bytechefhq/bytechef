import McpServersLeftSidebarNav from '@/ee/pages/embedded/mcp-servers/components/McpServersLeftSidebarNav';
import {render, screen, within} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    tagsData: undefined as {embeddedMcpServerTags: Array<{id: string; name: string}>} | undefined,
    tagsIsLoading: false,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useEmbeddedMcpServerTagsQuery: () => ({data: hoisted.tagsData, isLoading: hoisted.tagsIsLoading}),
    useMcpIntegrationInstanceConfigurationsQuery: () => ({
        data: {
            mcpIntegrationInstanceConfigurations: [
                {integration: {id: '3', name: 'Affinity'}, mcpServerId: '1'},
                {integration: {id: '4', name: 'Orphan'}, mcpServerId: '99'},
                null,
            ],
        },
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {name: 'httpClient', title: 'HTTP Client'},
            {name: 'slack', title: 'Slack'},
        ],
        isLoading: false,
    }),
}));

const renderNav = (url = '/embedded/mcp-servers') =>
    render(
        <MemoryRouter initialEntries={[url]}>
            <McpServersLeftSidebarNav allComponentNames={['httpClient']} validMcpServerIds={new Set(['1'])} />
        </MemoryRouter>
    );

describe('McpServersLeftSidebarNav', () => {
    beforeEach(() => {
        hoisted.tagsData = {
            embeddedMcpServerTags: [
                {id: '10', name: 'crm'},
                {id: '11', name: 'support'},
            ],
        };
        hoisted.tagsIsLoading = false;
    });

    it('lists the embedded MCP server tags as filter links', () => {
        renderNav();

        const tagsNav = screen.getByLabelText('Tags');

        expect(within(tagsNav).getByRole('link', {name: 'crm'})).toHaveAttribute(
            'href',
            '/embedded/mcp-servers?tagId=10'
        );
        expect(within(tagsNav).getByRole('link', {name: 'support'})).toHaveAttribute(
            'href',
            '/embedded/mcp-servers?tagId=11'
        );
    });

    it('marks the tag selected in the URL as current', () => {
        renderNav('/embedded/mcp-servers?tagId=11');

        const tagsNav = screen.getByLabelText('Tags');

        expect(within(tagsNav).getByRole('link', {name: 'support'})).toHaveAttribute('aria-current', 'page');
        expect(within(tagsNav).getByRole('link', {name: 'crm'})).not.toHaveAttribute('aria-current');
    });

    it('shows the empty message when no embedded tags are defined', () => {
        hoisted.tagsData = undefined;

        renderNav();

        expect(screen.getByText('No defined tags.')).toBeInTheDocument();
    });

    it('shows a loading skeleton while the embedded tags load', () => {
        hoisted.tagsIsLoading = true;

        renderNav();

        expect(within(screen.getByLabelText('Tags')).getByTestId('left-sidebar-nav-skeleton')).toBeInTheDocument();
    });

    it('lists only components and integrations that belong to the visible servers', () => {
        renderNav();

        expect(
            within(screen.getByLabelText('Components')).getByRole('link', {name: 'HTTP Client'})
        ).toBeInTheDocument();
        expect(
            within(screen.getByLabelText('Components')).queryByRole('link', {name: 'Slack'})
        ).not.toBeInTheDocument();
        expect(within(screen.getByLabelText('Integrations')).getByRole('link', {name: 'Affinity'})).toBeInTheDocument();
        expect(
            within(screen.getByLabelText('Integrations')).queryByRole('link', {name: 'Orphan'})
        ).not.toBeInTheDocument();
    });
});
