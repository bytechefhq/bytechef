import {Connection, ResponseError} from '@/ee/shared/middleware/embedded/public';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionsView from '../views/ConnectionsView';

const {deleteHubConnectionMutateMock, useGetComponentDefinitionsQueryMock, useGetConnectionsQueryMock} = vi.hoisted(
    () => ({
        deleteHubConnectionMutateMock: vi.fn(),
        useGetComponentDefinitionsQueryMock: vi.fn(),
        useGetConnectionsQueryMock: vi.fn(),
    })
);

vi.mock('@/ee/pages/embedded/automation-hub/queries/automationHub.queries', () => ({
    useGetConnectionsQuery: useGetConnectionsQueryMock,
}));

vi.mock('@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations', () => ({
    useDeleteHubConnectionMutation: () => ({mutate: deleteHubConnectionMutateMock}),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: useGetComponentDefinitionsQueryMock,
}));

vi.mock('@/ee/pages/embedded/automation-hub/views/components/HubConnectionDialog', () => ({
    default: ({
        componentName,
        existingConnectionCredentialStatus,
        existingConnectionId,
    }: {
        componentName: string;
        existingConnectionCredentialStatus?: string;
        existingConnectionId?: number;
    }) => (
        <div data-testid="hub-connection-dialog">
            reconnect:{componentName}:{existingConnectionId}:{existingConnectionCredentialStatus ?? 'none'}
        </div>
    ),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="component icon" src={src} />,
}));

const slackConnection: Connection = {
    componentName: 'slack',
    connectionVersion: 1,
    createdDate: new Date('2024-01-15T00:00:00Z'),
    id: 1,
    name: 'My Slack',
};

const hubspotConnection: Connection = {
    componentName: 'hubspot',
    connectionVersion: 1,
    createdDate: new Date('2024-02-20T00:00:00Z'),
    id: 2,
    name: 'My HubSpot',
};

const renderView = () => render(<ConnectionsView />);

describe('ConnectionsView', () => {
    beforeEach(() => {
        deleteHubConnectionMutateMock.mockReset();
        useGetComponentDefinitionsQueryMock.mockReset();
        useGetConnectionsQueryMock.mockReset();

        useGetConnectionsQueryMock.mockReturnValue({
            data: [slackConnection, hubspotConnection],
            error: null,
            isLoading: false,
        });

        useGetComponentDefinitionsQueryMock.mockReturnValue({
            data: [
                {icon: '<svg>slack</svg>', name: 'slack', title: 'Slack'},
                {icon: '<svg>hubspot</svg>', name: 'hubspot', title: 'HubSpot'},
            ],
            error: null,
            isLoading: false,
        });
    });

    it('renders no page heading of its own, because the tab already names the section', () => {
        renderView();

        expect(screen.queryByRole('heading', {name: 'Connections'})).not.toBeInTheDocument();
    });

    it('shows a row per connection with its name and component title', () => {
        renderView();

        const slackRow = screen.getByRole('row', {name: /My Slack/});

        expect(within(slackRow).getByText('Slack')).toBeInTheDocument();

        const hubspotRow = screen.getByRole('row', {name: /My HubSpot/});

        expect(within(hubspotRow).getByText('HubSpot')).toBeInTheDocument();
    });

    it('deletes a connection via useDeleteHubConnectionMutation after confirming', async () => {
        const user = userEvent.setup();

        renderView();

        await user.click(
            within(screen.getByRole('row', {name: /My Slack/})).getByRole('button', {name: 'Connection actions'})
        );
        await user.click(screen.getByRole('menuitem', {name: /delete/i}));
        await user.click(screen.getByRole('button', {name: 'Remove'}));

        expect(deleteHubConnectionMutateMock).toHaveBeenCalledWith(1, expect.anything());
    });

    it('shows an inline message when deleting a connection still used by an automation', async () => {
        const user = userEvent.setup();

        deleteHubConnectionMutateMock.mockImplementation(
            (_id: number, options?: {onError?: (error: unknown) => void}) => {
                options?.onError?.(
                    new ResponseError(
                        new Response(JSON.stringify({reason: 'CONNECTION_IS_USED'}), {status: 409}),
                        'Response returned an error code'
                    )
                );
            }
        );

        renderView();

        await user.click(
            within(screen.getByRole('row', {name: /My Slack/})).getByRole('button', {name: 'Connection actions'})
        );
        await user.click(screen.getByRole('menuitem', {name: /delete/i}));
        await user.click(screen.getByRole('button', {name: 'Remove'}));

        expect(await screen.findByText('This connection is still used by an enabled automation.')).toBeInTheDocument();
    });

    it('shows a generic inline message for a delete failure other than CONNECTION_IS_USED', async () => {
        const user = userEvent.setup();

        deleteHubConnectionMutateMock.mockImplementation(
            (_id: number, options?: {onError?: (error: unknown) => void}) => {
                options?.onError?.(new Error('Network error'));
            }
        );

        renderView();

        await user.click(
            within(screen.getByRole('row', {name: /My Slack/})).getByRole('button', {name: 'Connection actions'})
        );
        await user.click(screen.getByRole('menuitem', {name: /delete/i}));
        await user.click(screen.getByRole('button', {name: 'Remove'}));

        expect(await screen.findByText('Unable to delete this connection. Please try again.')).toBeInTheDocument();
        expect(screen.queryByText('This connection is still used by an enabled automation.')).not.toBeInTheDocument();
    });

    it('opens HubConnectionDialog for the same component and connection id via Reconnect', async () => {
        const user = userEvent.setup();

        renderView();

        await user.click(
            within(screen.getByRole('row', {name: /My HubSpot/})).getByRole('button', {name: 'Connection actions'})
        );
        await user.click(screen.getByRole('menuitem', {name: /reconnect/i}));

        expect(screen.getByTestId('hub-connection-dialog')).toHaveTextContent('reconnect:hubspot:2:none');
    });

    it('flags a connection whose credentials were rejected and passes that status to the reconnect dialog', async () => {
        const user = userEvent.setup();

        useGetConnectionsQueryMock.mockReturnValue({
            data: [{...slackConnection, credentialStatus: 'INVALID', editable: true}, hubspotConnection],
            error: null,
            isLoading: false,
        });

        renderView();

        expect(within(screen.getByRole('row', {name: /My Slack/})).getByText('Reconnect needed')).toBeInTheDocument();
        expect(within(screen.getByRole('row', {name: /My HubSpot/})).queryByText('Reconnect needed')).toBeNull();

        await user.click(
            within(screen.getByRole('row', {name: /My Slack/})).getByRole('button', {name: 'Connection actions'})
        );
        await user.click(screen.getByRole('menuitem', {name: /reconnect/i}));

        expect(screen.getByTestId('hub-connection-dialog')).toHaveTextContent('reconnect:slack:1:INVALID');
    });

    it('marks an owned connection the admin also shared, and still lets its owner act on it', () => {
        useGetConnectionsQueryMock.mockReturnValue({
            data: [
                {
                    componentName: 'slack',
                    createdDate: new Date('2025-01-02'),
                    editable: true,
                    id: 1,
                    name: 'My Slack',
                    shared: true,
                },
            ],
            error: null,
            isLoading: false,
        });

        renderView();

        expect(within(screen.getByRole('row', {name: /My Slack/})).getByText('Shared')).toBeInTheDocument();
        expect(
            within(screen.getByRole('row', {name: /My Slack/})).getByRole('button', {name: 'Connection actions'})
        ).toBeInTheDocument();
    });

    it('offers no actions on a connection this user does not own', async () => {
        const user = userEvent.setup();

        useGetConnectionsQueryMock.mockReturnValue({
            data: [
                {componentName: 'slack', createdDate: new Date('2025-01-02'), editable: true, id: 1, name: 'My Slack'},
                {
                    componentName: 'slack',
                    createdDate: new Date('2025-01-02'),
                    editable: false,
                    id: 2,
                    name: 'Team Slack',
                    shared: true,
                },
            ],
            error: null,
            isLoading: false,
        });

        renderView();

        const sharedRow = screen.getByRole('row', {name: /Team Slack/});

        expect(within(sharedRow).queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
        expect(within(sharedRow).getByText('Shared')).toBeInTheDocument();

        const ownedRow = screen.getByRole('row', {name: /My Slack/});

        await user.click(within(ownedRow).getByRole('button', {name: 'Connection actions'}));

        expect(screen.getByRole('menuitem', {name: /reconnect/i})).toBeInTheDocument();
    });
});
