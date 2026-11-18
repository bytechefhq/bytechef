import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {Connections} from '../Connections';

const hoisted = vi.hoisted(() => ({
    adminConnections: [] as Connection[],
    connectedUser: undefined as {environmentId: number; externalId: string; id: number; name: string} | undefined,
    connectedUserConnections: [] as Connection[],
    connectedUserIsLoading: false,
    useGetAllConnectedUserConnectionsQuery: vi.fn(),
    useGetConnectionsQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetAllConnectedUserConnectionsQuery: (connectedUserId: number, enabled?: boolean) => {
        hoisted.useGetAllConnectedUserConnectionsQuery(connectedUserId, enabled);

        return {
            data: enabled ? hoisted.connectedUserConnections : undefined,
            error: null,
            isLoading: false,
        };
    },
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
    useGetConnectionsQuery: (filters: unknown, enabled?: boolean) => {
        hoisted.useGetConnectionsQuery(filters, enabled);

        return {data: enabled === false ? undefined : hoisted.adminConnections, error: null, isLoading: false};
    },
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    useGetConnectedUserQuery: (id: number, enabled?: boolean) => ({
        data:
            enabled && !hoisted.connectedUserIsLoading
                ? {...hoisted.connectedUser, integrationInstances: []}
                : undefined,
        error: null,
        isLoading: !!enabled && hoisted.connectedUserIsLoading,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {name: 'gmail', title: 'Gmail'},
            {name: 'slack', title: 'Slack'},
        ],
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useCreateConnectionMutation: vi.fn(),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: () => <div data-testid="connection-dialog" />,
}));

vi.mock('@/ee/pages/embedded/connections/components/ConnectedUserFilterNav', () => ({
    default: () => <div data-testid="connected-user-filter-nav" />,
    getConnectedUserLabel: ({externalId, name}: {externalId: string; name?: string}) => name || externalId,
}));

vi.mock('../components/connection-list/ConnectionList', () => ({
    default: ({connectedUserId, connections}: {connectedUserId?: number; connections: Connection[]}) => (
        <ul data-connected-user-id={connectedUserId ?? ''} data-testid="connection-list">
            {connections.map((connection) => (
                <li key={connection.id}>{connection.name}</li>
            ))}
        </ul>
    ),
}));

const createConnection = (id: number, componentName: string, tagIds: number[] = []): Connection => ({
    componentName,
    connectionVersion: 1,
    id,
    name: `Connection ${id}`,
    origin: ConnectionOriginEnum.SelfServe,
    parameters: {},
    tags: tagIds.map((tagId) => ({id: tagId, name: `tag ${tagId}`})),
});

const LocationDisplay = () => {
    const location = useLocation();

    return <div data-testid="location-search">{location.search}</div>;
};

const renderPage = (initialEntry: string) =>
    render(
        <TooltipProvider>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route
                        element={
                            <>
                                <Connections />

                                <LocationDisplay />
                            </>
                        }
                        path="/"
                    />
                </Routes>
            </MemoryRouter>
        </TooltipProvider>
    );

describe('Connections', () => {
    beforeEach(() => {
        windowResizeObserver();

        environmentStore.setState({currentEnvironmentId: 1});

        hoisted.adminConnections = [createConnection(1, 'slack')];
        hoisted.connectedUser = {environmentId: 1, externalId: 'acme-1', id: 42, name: 'Acme Corp'};
        hoisted.connectedUserIsLoading = false;
        hoisted.connectedUserConnections = [
            createConnection(10, 'gmail', [3]),
            createConnection(11, 'gmail'),
            createConnection(12, 'slack', [3]),
        ];
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    it('lists admin connections without a connected user', () => {
        renderPage('/');

        expect(screen.getByTestId('connection-list')).toHaveTextContent('Connection 1');
        expect(hoisted.useGetAllConnectedUserConnectionsQuery).toHaveBeenLastCalledWith(0, false);
    });

    it('lists only the connected user connections of the selected component', () => {
        renderPage('/?connectedUserId=42&componentName=gmail');

        const connectionList = screen.getByTestId('connection-list');

        expect(connectionList).toHaveAttribute('data-connected-user-id', '42');
        expect(connectionList).toHaveTextContent('Connection 10');
        expect(connectionList).toHaveTextContent('Connection 11');
        expect(connectionList).not.toHaveTextContent('Connection 12');
        expect(connectionList).not.toHaveTextContent(/Connection 1(?!\d)/);
        expect(hoisted.useGetConnectionsQuery.mock.calls.every(([, enabled]) => enabled === false)).toBe(true);
        expect(screen.getByText('User: Acme Corp')).toBeInTheDocument();
    });

    it('lists only the connected user connections with the selected tag', () => {
        renderPage('/?connectedUserId=42&tagId=3');

        const connectionList = screen.getByTestId('connection-list');

        expect(connectionList).toHaveTextContent('Connection 10');
        expect(connectionList).toHaveTextContent('Connection 12');
        expect(connectionList).not.toHaveTextContent('Connection 11');
    });

    it('names a tag found only on the connected user connections in the filter title', () => {
        renderPage('/?connectedUserId=42&tagId=3');

        expect(screen.getByText('Tags: tag 3')).toBeInTheDocument();
    });

    it('lists no connections until the connected user has loaded', () => {
        hoisted.connectedUserIsLoading = true;

        renderPage('/?connectedUserId=42');

        expect(screen.queryByTestId('connection-list')).not.toBeInTheDocument();
    });

    it('drops a connected user from another environment instead of listing their connections', async () => {
        hoisted.connectedUser = {environmentId: 2, externalId: 'acme-1', id: 42, name: 'Acme Corp'};

        renderPage('/?connectedUserId=42');

        expect(screen.queryByText('Connection 10')).not.toBeInTheDocument();

        await waitFor(
            () => {
                expect(screen.getByTestId('location-search')).not.toHaveTextContent('connectedUserId');
            },
            {interval: 5, timeout: 5000}
        );
    });

    it('shows the empty filter result for a connected user without connections', () => {
        hoisted.connectedUserConnections = [];

        renderPage('/?connectedUserId=42');

        expect(screen.queryByText('Create your first connection')).not.toBeInTheDocument();
        expect(screen.queryByTestId('connection-list')).not.toBeInTheDocument();
        expect(screen.getByTestId('connected-user-filter-nav')).toBeInTheDocument();
    });

    it('drops the connected user when the environment changes', async () => {
        renderPage('/?connectedUserId=42');

        act(() => {
            environmentStore.setState({currentEnvironmentId: 2});
        });

        await waitFor(
            () => {
                expect(screen.getByTestId('location-search')).not.toHaveTextContent('connectedUserId');
            },
            {interval: 5, timeout: 5000}
        );
    });
});
