import {TooltipProvider} from '@/components/ui/tooltip';
import {Connections} from '@/ee/pages/embedded/connections/Connections';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    grants: {} as Record<number, string[]>,
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
    useGetConnectionTagsQuery: () => ({data: [], error: null, isLoading: false}),
    useGetConnectionsQuery: () => ({
        data: [
            {
                active: false,
                componentName: 'slack',
                connectionVersion: 1,
                credentialStatus: 'VALID',
                environmentId: 2,
                id: 5,
                name: 'Slack connection',
                tags: [],
            },
        ],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{name: 'slack', title: 'Slack'}], isLoading: false}),
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
    default: ({triggerNode}: {triggerNode?: ReactNode}) => <>{triggerNode}</>,
}));

vi.mock('@/shared/components/connection/ConnectionsLeftSidebarNav', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/connections/components/ConnectionsFilterTitle', () => ({
    default: () => null,
}));

vi.mock('@/components/LazyLoadSVG/LazyLoadSVG', () => ({
    default: () => null,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right}: {right?: ReactNode}) => <div>{right}</div>,
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;

const renderPage = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <Connections />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('embedded Connections', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.grants = {};
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

        renderPage();

        expect(screen.getByText('Slack connection')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New Connection'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Connection actions'})).not.toBeInTheDocument();
    });

    it('testConnectionControlsFollowTheirOwnScopes', async () => {
        hoisted.grants = {[PRODUCTION]: ['CONNECTION_CREATE', 'CONNECTION_EDIT', 'CONNECTION_VIEW']};

        renderPage();

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

        renderPage();

        expect(screen.getByRole('button', {name: 'New Connection'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Connection actions'}));

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });
});
