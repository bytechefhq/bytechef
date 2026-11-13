import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserSheetPanelConnectionList from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelConnectionList';
import {Connection} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    allConnectedUserConnectionsQuery: vi.fn(),
    componentDefinitionsQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    useGetAllConnectedUserConnectionsQuery: hoisted.allConnectedUserConnectionsQuery,
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

const renderConnectionList = (connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[] = []) =>
    render(
        <TooltipProvider>
            <ConnectedUserSheetPanelConnectionList
                connectedUserId={7}
                connectedUserIntegrationInstances={connectedUserIntegrationInstances}
            />
        </TooltipProvider>
    );

const getConnectionRow = (name: string) => screen.getByText(name).closest('li') as HTMLElement;

describe('ConnectedUserSheetPanelConnectionList', () => {
    beforeEach(() => {
        hoisted.allConnectedUserConnectionsQuery.mockReset();
        hoisted.componentDefinitionsQuery.mockReset();

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

    it('explains the origin badge in a tooltip', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await userEvent.hover(within(getConnectionRow('Provisioned HubSpot')).getByText('Provisioned'));

        expect(
            (await screen.findAllByText('An admin created this connection for the connected user.')).length
        ).toBeGreaterThan(0);
    });

    it('offers Credentials only on provisioned connections', () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList(integrationInstances);

        expect(
            within(getConnectionRow('Provisioned HubSpot')).getByRole('button', {name: 'Credentials'})
        ).toBeEnabled();
        expect(within(getConnectionRow('Own Slack')).queryByRole('button')).not.toBeInTheDocument();
        expect(within(getConnectionRow('Shared GitHub')).getByRole('button', {name: 'Credentials'})).toBeDisabled();
    });

    it('offers Credentials on a connection provisioned for automations', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await userEvent.click(
            within(getConnectionRow('Provisioned HubSpot')).getByRole('button', {name: 'Credentials'})
        );

        expect(screen.getByText('Credentials of connection 2 of connected user 7')).toBeInTheDocument();
        expect(screen.queryByText(/Credentials of integration instance/)).not.toBeInTheDocument();
    });

    it('explains that a shared connection is edited on the Connections page', async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList();

        await userEvent.hover(
            within(getConnectionRow('Shared GitHub')).getByRole('button', {name: 'Credentials'}).parentElement!
        );

        expect(
            (await screen.findAllByText('Shared connections are edited on the Connections page')).length
        ).toBeGreaterThan(0);
    });

    it("opens the credentials of the connection's integration instance", async () => {
        hoisted.allConnectedUserConnectionsQuery.mockReturnValue({data: connections, isLoading: false});

        renderConnectionList(integrationInstances);

        await userEvent.click(
            within(getConnectionRow('Provisioned HubSpot')).getByRole('button', {name: 'Credentials'})
        );

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
        expect(within(connectionRow).queryByRole('button')).not.toBeInTheDocument();
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
