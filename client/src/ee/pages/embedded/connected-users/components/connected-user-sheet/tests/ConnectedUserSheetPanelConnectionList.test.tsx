import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserSheetPanelConnectionList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelConnectionList';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    allConnectedUserConnectionsQuery: vi.fn(),
    componentDefinitionsQuery: vi.fn(),
    deleteConnectedUserConnection: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connections: ['embedded_connections']},
    useGetAllConnectedUserConnectionsQuery: hoisted.allConnectedUserConnectionsQuery,
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useDeleteConnectedUserConnectionMutation: (connectedUserId: number, mutationProps?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (connectionId: number) => {
            hoisted.deleteConnectedUserConnection(connectedUserId, connectionId);

            mutationProps?.onSuccess?.();
        },
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: hoisted.componentDefinitionsQuery,
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserIntegrationCredentialsDialog',
    () => ({
        default: ({integrationInstance}: {integrationInstance: ConnectedUserIntegrationInstance}) => (
            <div>{`Credentials of integration instance ${integrationInstance.id}`}</div>
        ),
    })
);

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionCredentialsDialog',
    () => ({
        default: ({connectedUserId, connection}: {connectedUserId: number; connection: Connection}) => (
            <div>{`Credentials of connection ${connection.id} of connected user ${connectedUserId}`}</div>
        ),
    })
);

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="component icon" src={src} />,
}));

const connections: Connection[] = [
    {
        active: true,
        componentName: 'slack',
        credentialStatus: 'VALID',
        id: 1,
        name: 'Own Slack',
        origin: 'SELF_SERVE',
    } as Connection,
    {
        active: true,
        componentName: 'hubspot',
        credentialStatus: 'INVALID',
        id: 2,
        name: 'Provisioned HubSpot',
        origin: 'PROVISIONED',
    } as Connection,
    {
        active: true,
        componentName: 'github',
        credentialStatus: 'VALID',
        id: 3,
        name: 'Shared GitHub',
        origin: 'INTERNAL',
        shared: true,
    } as Connection,
];

const integrationInstances: ConnectedUserIntegrationInstance[] = [
    {connectionId: 1, id: 10, provisioned: false},
    {connectionId: 2, id: 20, provisioned: true},
];

const queryClient = new QueryClient();

const renderConnectionList = (connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[] = []) =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <ConnectedUserSheetPanelConnectionList
                    connectedUserId={7}
                    connectedUserIntegrationInstances={connectedUserIntegrationInstances}
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

const getConnectionRow = (name: string) => screen.getByText(name).closest('li') as HTMLElement;

const openActionsMenu = (name: string) =>
    userEvent.click(within(getConnectionRow(name)).getByRole('button', {name: 'More actions'}));

describe('ConnectedUserSheetPanelConnectionList', () => {
    beforeEach(() => {
        hoisted.allConnectedUserConnectionsQuery.mockReset();
        hoisted.componentDefinitionsQuery.mockReset();
        hoisted.deleteConnectedUserConnection.mockReset();

        hoisted.componentDefinitionsQuery.mockReturnValue({
            data: [{icon: 'slack.svg', name: 'slack', title: 'Slack'}],
        });
    });

    it('reads every connection of the connected user in one request', () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: undefined, isLoading: true});

        renderConnectionList();

        expect(hoisted.allConnectedUserConnectionsQuery).toHaveBeenCalledWith(7);
        expect(screen.getByRole('status', {name: 'Loading connections'})).toBeInTheDocument();
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });

    it('labels each connection with how the connected user came to have it', () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        expect(within(getConnectionRow('Own Slack')).getByText('Self-created')).toBeInTheDocument();
        expect(within(getConnectionRow('Provisioned HubSpot')).getByText('Provisioned')).toBeInTheDocument();
        expect(within(getConnectionRow('Shared GitHub')).getByText('Internal')).toBeInTheDocument();
    });

    it('marks a connection an integration of the user uses as Integration and every other one as Automation', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList([{connectionId: 1, id: 10, provisioned: false}]);

        expect(within(getConnectionRow('Own Slack')).getByText('Integration')).toBeInTheDocument();
        expect(within(getConnectionRow('Provisioned HubSpot')).getByText('Automation')).toBeInTheDocument();
        expect(within(getConnectionRow('Shared GitHub')).getByText('Automation')).toBeInTheDocument();

        await userEvent.hover(within(getConnectionRow('Shared GitHub')).getByText('Automation'));

        expect(
            (await screen.findAllByText('An admin connection automation configurations provide to their workflows'))
                .length
        ).toBeGreaterThan(0);
    });

    it('explains the origin badge in a tooltip', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await userEvent.hover(within(getConnectionRow('Provisioned HubSpot')).getByText('Provisioned'));

        expect(
            (await screen.findAllByText('An admin created this connection for the connected user.')).length
        ).toBeGreaterThan(0);
    });

    it('offers Credentials in the actions menu only on provisioned connections', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList(integrationInstances);

        expect(screen.queryByRole('button', {name: 'Credentials'})).not.toBeInTheDocument();

        await openActionsMenu('Provisioned HubSpot');

        expect(screen.getByRole('menuitem', {name: 'Credentials'})).not.toHaveAttribute('aria-disabled');

        await userEvent.keyboard('{Escape}');
        await openActionsMenu('Own Slack');

        expect(screen.queryByRole('menuitem', {name: 'Credentials'})).not.toBeInTheDocument();

        await userEvent.keyboard('{Escape}');
        await openActionsMenu('Shared GitHub');

        expect(screen.getByRole('menuitem', {name: 'Credentials'})).toHaveAttribute('aria-disabled', 'true');
    });

    it('offers Delete on self-created and provisioned connections but not on shared ones', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await openActionsMenu('Own Slack');

        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();

        await userEvent.keyboard('{Escape}');
        await openActionsMenu('Shared GitHub');

        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();

        await userEvent.keyboard('{Escape}');
        await openActionsMenu('Provisioned HubSpot');

        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('lists Credentials before Delete in the actions menu', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await openActionsMenu('Provisioned HubSpot');

        expect(screen.getAllByRole('menuitem').map((menuItem) => menuItem.textContent)).toEqual([
            'Credentials',
            'Delete',
        ]);
    });

    it('deletes a connection of the connected user after the admin confirms', async () => {
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await openActionsMenu('Provisioned HubSpot');
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.deleteConnectedUserConnection).not.toHaveBeenCalled();
        expect(
            screen.getByText(
                'This action cannot be undone. This will permanently delete the connection Provisioned HubSpot.'
            )
        ).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectedUserConnection).toHaveBeenCalledWith(7, 2);
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['embedded_connections']});
        expect(
            screen.queryByText(
                'This action cannot be undone. This will permanently delete the connection Provisioned HubSpot.'
            )
        ).not.toBeInTheDocument();
    });

    it('offers Credentials on a connection provisioned for automations', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await openActionsMenu('Provisioned HubSpot');
        await userEvent.click(screen.getByRole('menuitem', {name: 'Credentials'}));

        expect(screen.getByText('Credentials of connection 2 of connected user 7')).toBeInTheDocument();
        expect(screen.queryByText(/Credentials of integration instance/)).not.toBeInTheDocument();
    });

    it('explains that a shared connection is edited on the Connections page', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await openActionsMenu('Shared GitHub');
        await userEvent.hover(screen.getByRole('menuitem', {name: 'Credentials'}).parentElement!);

        expect(
            (await screen.findAllByText('Shared connections are edited on the Connections page')).length
        ).toBeGreaterThan(0);
    });

    it("opens the credentials of the connection's integration instance", async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList(integrationInstances);

        await openActionsMenu('Provisioned HubSpot');
        await userEvent.click(screen.getByRole('menuitem', {name: 'Credentials'}));

        expect(screen.getByText('Credentials of integration instance 20')).toBeInTheDocument();
        expect(screen.queryByText(/Credentials of connection/)).not.toBeInTheDocument();
    });

    it('shows no origin badge when the origin is unknown', () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({
            data: [{active: true, componentName: 'slack', credentialStatus: 'VALID', id: 4, name: 'Plain Slack'}],
            isLoading: false,
        });

        renderConnectionList();

        const connectionRow = getConnectionRow('Plain Slack');

        expect(within(connectionRow).queryByText('Self-created')).not.toBeInTheDocument();
        expect(within(connectionRow).queryByText('Provisioned')).not.toBeInTheDocument();
        expect(within(connectionRow).queryByText('Internal')).not.toBeInTheDocument();
    });

    it("shows each connection's credential status under its name", async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        expect(within(getConnectionRow('Own Slack')).getByText('Account Connected')).toBeInTheDocument();
        expect(within(getConnectionRow('Provisioned HubSpot')).getByText('Account Errors')).toBeInTheDocument();

        await userEvent.hover(within(getConnectionRow('Own Slack')).getByText('Account Connected'));

        expect((await screen.findAllByText("The user's Slack credentials are valid")).length).toBeGreaterThan(0);
    });

    it('shows the origin badge without an active state or edit actions', () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        const connectionRow = getConnectionRow('Own Slack');

        expect(within(connectionRow).getByText('Self-created')).toBeInTheDocument();
        expect(within(connectionRow).queryByText(/active/i)).not.toBeInTheDocument();
        expect(within(connectionRow).queryByRole('button', {name: /edit/i})).not.toBeInTheDocument();
    });

    it("shows the connection's component icon", () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        expect(within(getConnectionRow('Own Slack')).getByRole('img', {name: 'component icon'})).toHaveAttribute(
            'src',
            'slack.svg'
        );
    });

    it('shows an empty state when the connected user has no connections', () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: [], isLoading: false});

        renderConnectionList();

        expect(screen.getByText('No connections yet')).toBeInTheDocument();
    });

    it('lets the admin retry when the connections cannot be loaded', () => {
        const refetch = vi.fn();

        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({
            data: undefined,
            isError: true,
            isLoading: false,
            refetch,
        });

        renderConnectionList();

        expect(screen.getByText('Connections could not be loaded.')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });
});
