import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserSheetPanelMcpServerList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelMcpServerList';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render as testingLibraryRender, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const render = (ui: ReactElement) => testingLibraryRender(ui, {wrapper: TooltipProvider});

const hoisted = vi.hoisted(() => ({
    connectedUserMcpServersQuery: vi.fn(),
    provisionableMcpServers: [{id: '5'}] as unknown[],
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useConnectedUserMcpServersQuery: hoisted.connectedUserMcpServersQuery,
    useProvisionableConnectedUserMcpServersQuery: () => ({
        data: {provisionableConnectedUserMcpServers: hoisted.provisionableMcpServers},
    }),
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-mcp-server-dialog/ProvisionMcpServerDialog',
    () => ({
        default: ({connectedUserId}: {connectedUserId: number}) => <div>{`provision dialog ${connectedUserId}`}</div>,
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem',
    () => ({
        default: ({mcpServer}: {mcpServer: {id: string}}) => <div>{`mcp server ${mcpServer.id}`}</div>,
    })
);

const renderPanel = () =>
    render(
        <MemoryRouter>
            <QueryClientProvider client={new QueryClient()}>
                <ConnectedUserSheetPanelMcpServerList connectedUserId={1050} connectedUserIntegrationInstances={[]} />
            </QueryClientProvider>
        </MemoryRouter>
    );

describe('ConnectedUserSheetPanelMcpServerList', () => {
    beforeEach(() => {
        hoisted.provisionableMcpServers = [{id: '5'}];

        hoisted.connectedUserMcpServersQuery.mockReturnValue({
            data: {connectedUserMcpServers: [{id: '1'}, {id: '2'}]},
            isLoading: false,
        });
    });

    it("lists the user's MCP servers", () => {
        renderPanel();

        expect(hoisted.connectedUserMcpServersQuery).toHaveBeenCalledWith({connectedUserId: '1050'});
        expect(screen.getByText('mcp server 1')).toBeInTheDocument();
        expect(screen.getByText('mcp server 2')).toBeInTheDocument();
    });

    it('shows a loading state and then an empty state', () => {
        hoisted.connectedUserMcpServersQuery.mockReturnValue({data: undefined, isLoading: true});

        const {rerender} = renderPanel();

        expect(screen.getByRole('status', {name: 'Loading MCP servers'})).toBeInTheDocument();
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();

        hoisted.connectedUserMcpServersQuery.mockReturnValue({data: {connectedUserMcpServers: []}, isLoading: false});

        rerender(
            <MemoryRouter>
                <QueryClientProvider client={new QueryClient()}>
                    <ConnectedUserSheetPanelMcpServerList
                        connectedUserId={1050}
                        connectedUserIntegrationInstances={[]}
                    />
                </QueryClientProvider>
            </MemoryRouter>
        );

        expect(screen.getByRole('heading', {name: 'No MCP servers yet'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Provision MCP Server'})).toBeInTheDocument();
    });

    it('offers to open the MCP Servers page when no MCP server is defined', () => {
        hoisted.connectedUserMcpServersQuery.mockReturnValue({data: {connectedUserMcpServers: []}, isLoading: false});
        hoisted.provisionableMcpServers = [];

        renderPanel();

        expect(screen.getByText(/No MCP servers are defined yet/)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Go to MCP Servers'})).toBeInTheDocument();
    });

    it('opens the provisioning dialog from the Provision MCP Server button', async () => {
        renderPanel();

        expect(screen.queryByText('provision dialog 1050')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Provision MCP Server'}));

        expect(screen.getByText('provision dialog 1050')).toBeInTheDocument();
    });

    it('disables Provision MCP Server and explains why when every server is already provisioned', async () => {
        hoisted.provisionableMcpServers = [{id: '5', provisioned: true}];

        renderPanel();

        const provisionButton = screen.getByRole('button', {name: 'Provision MCP Server'});

        expect(provisionButton).toBeDisabled();

        await userEvent.hover(provisionButton.parentElement!);

        expect(
            await screen.findByRole('tooltip', {name: 'Every MCP server is already provisioned for this user.'})
        ).toBeInTheDocument();
    });
});
