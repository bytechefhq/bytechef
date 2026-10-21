import {TooltipProvider} from '@/components/ui/tooltip';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {ComponentProps, ReactNode} from 'react';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {Connections} from '../Connections';

const hoisted = vi.hoisted(() => ({
    adminConnections: [] as Connection[],
    connectedUser: undefined as {environmentId: number; externalId: string; id: number; name: string} | undefined,
    connectedUserConnections: [] as Connection[],
    connectedUserConnectionsError: null as Error | null,
    connectedUserIsLoading: false,
    grants: {} as Record<number, string[]>,
    useGetAllConnectedUserConnectionsQuery: vi.fn(),
    useGetConnectionsQuery: vi.fn(),
    useRealConnectionList: false,
    useRealScopeHook: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            hoisted.useRealScopeHook
                ? actual.useHasEmbeddedScope(scope as never, environmentId)
                : (
                      hoisted.grants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
                  ).includes(scope),
    };
});

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {connectionTags: ['connectionTags'], connections: ['connections']},
    useGetAllConnectedUserConnectionsQuery: (connectedUserId: number, enabled?: boolean) => {
        hoisted.useGetAllConnectedUserConnectionsQuery(connectedUserId, enabled);

        return {
            data: enabled ? hoisted.connectedUserConnections : undefined,
            error: enabled ? hoisted.connectedUserConnectionsError : null,
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

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
    useGetConnectionComponentDefinitionQuery: () => ({data: {icon: '', name: 'slack', title: 'Slack'}}),
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    useCreateConnectionMutation: vi.fn(),
    useDeleteConnectionMutation: () => ({mutate: vi.fn()}),
    useUpdateConnectionMutation: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/connectionTags.mutations', () => ({
    useUpdateConnectionTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/components/connection/ConnectionDialog', () => ({
    default: ({triggerNode}: {triggerNode?: ReactNode}) => <div data-testid="connection-dialog">{triggerNode}</div>,
}));

vi.mock('@/components/LazyLoadSVG/LazyLoadSVG', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/connections/components/ConnectedUserFilterNav', () => ({
    default: () => <div data-testid="connected-user-filter-nav" />,
    getConnectedUserLabel: ({externalId, name}: {externalId: string; name?: string}) => name || externalId,
}));

vi.mock('../components/connection-list/ConnectionList', async (importOriginal) => {
    const actual = await importOriginal<typeof import('../components/connection-list/ConnectionList')>();
    const ActualConnectionList = actual.default;

    return {
        default: (props: ComponentProps<typeof ActualConnectionList>) =>
            hoisted.useRealConnectionList ? (
                <ActualConnectionList {...props} />
            ) : (
                <ul data-connected-user-id={props.connectedUserId ?? ''} data-testid="connection-list">
                    {props.connections.map((connection) => (
                        <li key={connection.id}>{connection.name}</li>
                    ))}
                </ul>
            ),
    };
});

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

        hoisted.grants = {1: ['CONNECTION_CREATE', 'CONNECTION_DELETE', 'CONNECTION_EDIT', 'CONNECTION_VIEW']};
        hoisted.useRealConnectionList = false;
        hoisted.useRealScopeHook = false;
        hoisted.adminConnections = [createConnection(1, 'slack')];
        hoisted.connectedUser = {environmentId: 1, externalId: 'acme-1', id: 42, name: 'Acme Corp'};
        hoisted.connectedUserIsLoading = false;
        hoisted.connectedUserConnectionsError = null;
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

        expect(screen.getByText('No Matching Connections')).toBeInTheDocument();
        expect(screen.queryByText('Create your first connection')).not.toBeInTheDocument();
        expect(screen.queryByTestId('connection-list')).not.toBeInTheDocument();
        expect(screen.getByTestId('connected-user-filter-nav')).toBeInTheDocument();
    });

    it("shows an error instead of an empty list when the connected user's connections cannot be loaded", () => {
        hoisted.connectedUserConnectionsError = new Error('boom');

        renderPage('/?connectedUserId=42');

        expect(screen.getByText('Some error occurred.')).toBeInTheDocument();
        expect(screen.queryByText('No Matching Connections')).not.toBeInTheDocument();
        expect(screen.queryByTestId('connection-list')).not.toBeInTheDocument();
    });

    it('lists the admin connections and drops a connected user id that is not a number', async () => {
        renderPage('/?connectedUserId=abc');

        expect(screen.getByText('Connection 1')).toBeInTheDocument();
        expect(screen.getByTestId('connection-list')).toHaveAttribute('data-connected-user-id', '');
        expect(hoisted.useGetAllConnectedUserConnectionsQuery).not.toHaveBeenCalledWith(expect.anything(), true);

        await waitFor(
            () => {
                expect(screen.getByTestId('location-search')).not.toHaveTextContent('connectedUserId');
            },
            {interval: 5, timeout: 5000}
        );
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

const DEVELOPMENT = 0;
const PRODUCTION = 2;

describe('embedded Connections', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.adminConnections = [
            {
                active: false,
                componentName: 'slack',
                connectionVersion: 1,
                credentialStatus: 'VALID',
                environmentId: PRODUCTION,
                id: 5,
                name: 'Slack connection',
                parameters: {},
                tags: [],
            } as Connection,
        ];
        hoisted.connectedUser = undefined;
        hoisted.grants = {};
        hoisted.useRealConnectionList = true;
        hoisted.useRealScopeHook = false;

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        resetAll();
    });

    it('testAViewerSeesConnectionsWithoutCreateEditOrDelete', () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['CONNECTION_CREATE', 'CONNECTION_DELETE', 'CONNECTION_EDIT', 'CONNECTION_VIEW'],
            [PRODUCTION]: ['CONNECTION_VIEW'],
        };

        renderPage('/');

        expect(screen.getByText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Connection'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
    });

    it('testConnectionControlsFollowTheirOwnScopes', async () => {
        hoisted.grants = {[PRODUCTION]: ['CONNECTION_CREATE', 'CONNECTION_EDIT', 'CONNECTION_VIEW']};

        renderPage('/');

        expect(screen.getByRole('button', {name: 'New Connection'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testATenantAdminSeesEveryControl', async () => {
        hoisted.useRealScopeHook = true;

        act(() => {
            authenticationStore.setState({
                account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
                authenticated: true,
            });
        });

        renderPage('/');

        expect(screen.getByRole('button', {name: 'New Connection'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });
});
