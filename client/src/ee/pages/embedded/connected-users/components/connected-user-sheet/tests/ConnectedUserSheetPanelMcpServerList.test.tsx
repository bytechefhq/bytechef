import ConnectedUserSheetPanelMcpServerList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelMcpServerList';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    connectedUserMcpServersQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useConnectedUserMcpServersQuery: hoisted.connectedUserMcpServersQuery,
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
        <QueryClientProvider client={new QueryClient()}>
            <ConnectedUserSheetPanelMcpServerList connectedUserId={1050} connectedUserIntegrationInstances={[]} />
        </QueryClientProvider>
    );

describe('ConnectedUserSheetPanelMcpServerList', () => {
    beforeEach(() => {
        hoisted.connectedUserMcpServersQuery.mockReturnValue({
            data: {connectedUserMcpServers: [{id: '1'}, {id: '2'}]},
            isLoading: false,
        });
    });

    it("lists the user's MCP servers under the header", () => {
        renderPanel();

        expect(hoisted.connectedUserMcpServersQuery).toHaveBeenCalledWith({connectedUserId: '1050'});
        expect(screen.getByRole('heading', {name: 'MCP Servers'})).toBeInTheDocument();
        expect(screen.getByText('mcp server 1')).toBeInTheDocument();
        expect(screen.getByText('mcp server 2')).toBeInTheDocument();
    });

    it('shows a loading state and then an empty state', () => {
        hoisted.connectedUserMcpServersQuery.mockReturnValue({data: undefined, isLoading: true});

        const {rerender} = renderPanel();

        expect(screen.getByText('Loading...')).toBeInTheDocument();

        hoisted.connectedUserMcpServersQuery.mockReturnValue({data: {connectedUserMcpServers: []}, isLoading: false});

        rerender(
            <QueryClientProvider client={new QueryClient()}>
                <ConnectedUserSheetPanelMcpServerList connectedUserId={1050} connectedUserIntegrationInstances={[]} />
            </QueryClientProvider>
        );

        expect(screen.getByText("No MCP servers expose this user's integrations.")).toBeInTheDocument();
    });

    it('opens the provisioning dialog from the New MCP Server button', async () => {
        renderPanel();

        expect(screen.queryByText('provision dialog 1050')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'New MCP Server'}));

        expect(screen.getByText('provision dialog 1050')).toBeInTheDocument();
    });
});
