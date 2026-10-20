import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUsers from '@/ee/pages/embedded/connected-users/ConnectedUsers';
import ConnectedUserSheetActions from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetActions';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
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

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    ConnectedUserKeys: {connectedUser: (id: number) => ['connectedUser', id], connectedUsers: ['connectedUsers']},
    useGetConnectedUsersQuery: () => ({
        data: {content: [{enabled: true, id: 5, name: 'Jane', totalElements: 1}], totalElements: 1, totalPages: 1},
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheet', () => ({
    default: () => <div />,
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;

const renderPage = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ConnectedUsers />
            </TooltipProvider>
        </MemoryRouter>
    );

const openRowMenu = async () => {
    const menuTriggers = screen
        .getAllByRole('button')
        .filter((button) => button.getAttribute('aria-haspopup') === 'menu' && !button.getAttribute('aria-label'));

    await userEvent.click(menuTriggers[menuTriggers.length - 1]);
};

describe('ConnectedUsers', () => {
    beforeEach(() => {
        hoisted.useRealScopeHook = false;
        hoisted.grants = {};

        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });
    });

    it('testAViewerSeesConnectedUsersWithoutEnableOrDelete', async () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['CONNECTED_USER_DELETE', 'CONNECTED_USER_MANAGE', 'CONNECTED_USER_VIEW'],
            [PRODUCTION]: ['CONNECTED_USER_VIEW'],
        };

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        renderPage();

        expect(screen.getByText('Jane')).toBeInTheDocument();

        await openRowMenu();

        expect(await screen.findByText('Details')).toBeInTheDocument();
        expect(screen.queryByText('Disable')).not.toBeInTheDocument();
        expect(screen.queryByText('Enable')).not.toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testOnlyAnAdminDeletesAConnectedUser', async () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['CONNECTED_USER_MANAGE', 'CONNECTED_USER_VIEW'],
            [PRODUCTION]: ['CONNECTED_USER_DELETE', 'CONNECTED_USER_MANAGE', 'CONNECTED_USER_VIEW'],
        };

        const developmentRender = renderPage();

        await openRowMenu();

        expect(await screen.findByText('Disable')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();

        developmentRender.unmount();

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        renderPage();

        await openRowMenu();

        expect(await screen.findByText('Delete')).toBeInTheDocument();
    });

    it('testTheSheetActionsFollowManageAndDelete', async () => {
        hoisted.grants = {[DEVELOPMENT]: ['CONNECTED_USER_MANAGE', 'CONNECTED_USER_VIEW']};

        const manageRender = render(
            <ConnectedUserSheetActions connectedUser={{enabled: true, externalId: 'ext-5', id: 5}} />
        );

        await userEvent.click(screen.getByRole('button'));

        expect(await screen.findByText('Disable')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();

        manageRender.unmount();

        hoisted.grants = {[DEVELOPMENT]: ['CONNECTED_USER_VIEW']};

        render(<ConnectedUserSheetActions connectedUser={{enabled: true, externalId: 'ext-5', id: 5}} />);

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
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

        await openRowMenu();

        expect(await screen.findByText('Details')).toBeInTheDocument();
        expect(screen.getByText('Disable')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });
});
