import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationInstanceConfigurations from '@/ee/pages/embedded/integration-instance-configurations/IntegrationInstanceConfigurations';
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

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurationTags.queries', () => ({
    IntegrationInstanceConfigurationTagKeys: {integrationInstanceConfigurationTags: ['configurationTags']},
    useGetIntegrationInstanceConfigurationTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    IntegrationInstanceConfigurationKeys: {integrationInstanceConfigurations: ['integrationInstanceConfigurations']},
    useGetIntegrationInstanceConfigurationsQuery: () => ({
        data: [
            {
                enabled: true,
                id: 7,
                integration: {componentName: 'mailchimp', id: 1},
                integrationId: 1,
                integrationInstanceConfigurationWorkflows: [],
                integrationVersion: 1,
                name: 'Mailchimp production',
                tags: [{id: 1, name: 'crm'}],
            },
        ],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => ({
        data: [{componentName: 'mailchimp', id: 1, name: 'Mailchimp'}],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpIntegrationInstanceConfigurationsQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/components/read-only-workflow-editor/ReadOnlyWorkflowSheet', () => ({default: () => <div />}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList',
    () => ({default: () => null})
);

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: ({triggerNode}: {triggerNode?: React.ReactNode}) => (
            <div data-testid="configuration-dialog">{triggerNode}</div>
        ),
    })
);

const DEVELOPMENT = 0;
const PRODUCTION = 2;

const renderPage = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <IntegrationInstanceConfigurations />
            </TooltipProvider>
        </MemoryRouter>
    );

const openMenu = async () => {
    const menuTriggers = screen
        .getAllByRole('button')
        .filter((button) => button.getAttribute('aria-haspopup') === 'menu' && !button.getAttribute('aria-label'));

    await userEvent.click(menuTriggers[menuTriggers.length - 1]);
};

describe('IntegrationInstanceConfigurations', () => {
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

    it('testAProductionEditorEditsOnlyProductionConfigurations', async () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['INTEGRATION_INSTANCE_CONFIGURATION_VIEW'],
            [PRODUCTION]: ['INTEGRATION_INSTANCE_CONFIGURATION_EDIT', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW'],
        };

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        const productionRender = renderPage();

        expect(screen.getByText('Mailchimp production')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Add new tag'})).toBeInTheDocument();
        expect(screen.queryByText('New Instance Configuration')).not.toBeInTheDocument();

        await openMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Update Integration Version')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();

        productionRender.unmount();

        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        renderPage();

        expect(screen.getByRole('switch')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Add new tag'})).not.toBeInTheDocument();
        expect(
            screen
                .queryAllByRole('button')
                .filter(
                    (button) => button.getAttribute('aria-haspopup') === 'menu' && !button.getAttribute('aria-label')
                )
        ).toHaveLength(0);
    });

    it('testAConfigurationCreatorSeesNewInstanceConfigurationOnlyWithCreateInTheCurrentEnvironment', () => {
        hoisted.grants = {
            [DEVELOPMENT]: ['INTEGRATION_INSTANCE_CONFIGURATION_VIEW'],
            [PRODUCTION]: ['INTEGRATION_INSTANCE_CONFIGURATION_CREATE', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW'],
        };

        const developmentRender = renderPage();

        expect(screen.queryByText('New Instance Configuration')).not.toBeInTheDocument();

        developmentRender.unmount();

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });

        renderPage();

        expect(screen.getByText('New Instance Configuration')).toBeInTheDocument();
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

        expect(screen.getByText('New Instance Configuration')).toBeInTheDocument();
        expect(screen.getByRole('switch')).toBeEnabled();
        expect(screen.getByRole('button', {name: 'Add new tag'})).toBeInTheDocument();

        await openMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Update Integration Version')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });
});
