import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheetPanel from './ConnectedUserSheetPanel';
import ConnectedUserSheetPanelMcpServerList from './ConnectedUserSheetPanelMcpServerList';

const hoisted = vi.hoisted(() => ({
    isTenantAdmin: true,
    useConnectedUserMcpServersQuery: vi.fn(),
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useConnectedUserMcpServersQuery: hoisted.useConnectedUserMcpServersQuery,
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem',
    () => ({
        default: ({mcpServer}: {mcpServer: {name: string}}) => <div>{mcpServer.name}</div>,
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationList',
    () => ({
        default: () => null,
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelWorkflowList',
    () => ({
        default: () => null,
    })
);

const connectedUser = {id: 3, integrationInstances: [], name: 'Jane'} as unknown as ConnectedUser;

describe('ConnectedUserSheetPanel', () => {
    beforeEach(() => {
        hoisted.isTenantAdmin = true;

        hoisted.useConnectedUserMcpServersQuery.mockReset();
        hoisted.useConnectedUserMcpServersQuery.mockImplementation(
            (_variables: unknown, options?: {enabled?: boolean}) => ({
                data:
                    options?.enabled === false ? undefined : {connectedUserMcpServers: [{id: '5', name: 'Sales MCP'}]},
                isLoading: false,
            })
        );
    });

    it("lists the connected user's MCP servers to a tenant admin", async () => {
        render(<ConnectedUserSheetPanel connectedUser={connectedUser} />);

        await userEvent.click(screen.getByRole('tab', {name: 'MCP Servers'}));

        expect(screen.getByText('Sales MCP')).toBeInTheDocument();
    });

    it('hides the MCP Servers tab from a user who is not a tenant admin', () => {
        hoisted.isTenantAdmin = false;

        render(<ConnectedUserSheetPanel connectedUser={connectedUser} />);

        expect(screen.getByRole('tab', {name: 'Integrations'})).toBeInTheDocument();
        expect(screen.queryByRole('tab', {name: 'MCP Servers'})).not.toBeInTheDocument();
    });

    it("does not read the connected user's MCP servers for a user who is not a tenant admin", () => {
        hoisted.isTenantAdmin = false;

        render(<ConnectedUserSheetPanelMcpServerList connectedUserId={3} connectedUserIntegrationInstances={[]} />);

        expect(hoisted.useConnectedUserMcpServersQuery).toHaveBeenCalledWith({connectedUserId: '3'}, {enabled: false});
        expect(screen.queryByText('Sales MCP')).not.toBeInTheDocument();
    });
});
