import {TooltipProvider} from '@/components/ui/tooltip';
import AppEvents from '@/ee/pages/embedded/app-events/AppEvents';
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

vi.mock('@/ee/shared/queries/embedded/appEvents.queries', () => ({
    AppEventKeys: {appEvents: ['appEvents']},
    useGetAppEventsQuery: () => ({
        data: [{id: 3, name: 'order.created'}],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/mutations/embedded/appEvents.mutations', () => ({
    useCreateAppEventMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteAppEventMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateAppEventMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/pages/embedded/app-events/components/AppEventDialog', () => ({
    default: ({triggerNode}: {triggerNode?: React.ReactNode}) => (
        <div data-testid="app-event-dialog">{triggerNode}</div>
    ),
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;
const UNION = -1;

const grant = (union: string[]) => {
    hoisted.grants = {[DEVELOPMENT]: union, [PRODUCTION]: union, [UNION]: union};
};

const renderPage = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AppEvents />
            </TooltipProvider>
        </MemoryRouter>
    );

const openRowMenu = async () => {
    const menuTriggers = screen
        .getAllByRole('button')
        .filter((button) => button.getAttribute('aria-haspopup') === 'menu' && !button.getAttribute('aria-label'));

    await userEvent.click(menuTriggers[menuTriggers.length - 1]);
};

describe('AppEvents', () => {
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

    it('testAppEventCreateStaysDevelopmentOnlyAndNeedsAppEventCreate', () => {
        grant(['APP_EVENT_CREATE', 'APP_EVENT_VIEW']);

        const developmentRender = renderPage();

        expect(screen.getByText('New App Event')).toBeInTheDocument();

        developmentRender.unmount();

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        const productionRender = renderPage();

        expect(screen.queryByText('New App Event')).not.toBeInTheDocument();

        productionRender.unmount();

        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        grant(['APP_EVENT_VIEW']);

        renderPage();

        expect(screen.getByText('order.created')).toBeInTheDocument();
        expect(screen.queryByText('New App Event')).not.toBeInTheDocument();
    });

    it('testAppEventScopesAreCheckedOnTheUnionNotTheCurrentEnvironment', async () => {
        hoisted.grants = {
            [PRODUCTION]: ['APP_EVENT_CREATE', 'APP_EVENT_DELETE', 'APP_EVENT_EDIT', 'APP_EVENT_VIEW'],
            [UNION]: ['APP_EVENT_CREATE', 'APP_EVENT_DELETE', 'APP_EVENT_EDIT', 'APP_EVENT_VIEW'],
        };

        renderPage();

        expect(screen.getByText('New App Event')).toBeInTheDocument();

        await openRowMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('testNoAppEventControlsWhenOnlyDevelopmentHoldsTheScopes', () => {
        hoisted.grants = {[DEVELOPMENT]: ['APP_EVENT_CREATE', 'APP_EVENT_DELETE', 'APP_EVENT_EDIT', 'APP_EVENT_VIEW']};

        renderPage();

        expect(screen.queryByText('New App Event')).not.toBeInTheDocument();
    });

    it('testAViewerSeesAppEventsWithoutEditOrDelete', async () => {
        grant(['APP_EVENT_EDIT', 'APP_EVENT_VIEW']);

        const editorRender = renderPage();

        await openRowMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();

        editorRender.unmount();

        grant(['APP_EVENT_VIEW']);

        renderPage();

        expect(screen.getByText('order.created')).toBeInTheDocument();
        expect(
            screen
                .queryAllByRole('button')
                .filter(
                    (button) => button.getAttribute('aria-haspopup') === 'menu' && !button.getAttribute('aria-label')
                )
        ).toHaveLength(0);
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

        expect(screen.getByText('New App Event')).toBeInTheDocument();

        await openRowMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });
});
