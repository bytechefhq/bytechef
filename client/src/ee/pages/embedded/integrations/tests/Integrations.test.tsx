import {TooltipProvider} from '@/components/ui/tooltip';
import Integrations from '@/ee/pages/embedded/integrations/Integrations';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    grants: {} as Record<number, string[]>,
    useRealScopeHook: false,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            hoisted.useRealScopeHook
                ? actual.useHasEmbeddedScope(scope as never, environmentId)
                : (hoisted.grants[environmentId ?? 0] ?? []).includes(scope),
    };
});

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integration: (id: number) => ['integration', id], integrations: ['integrations']},
    useGetIntegrationsQuery: () => ({
        data: [
            {
                id: 1,
                integrationWorkflowIds: [10],
                lastIntegrationVersion: 1,
                name: 'Mailchimp',
                tags: [{id: 1, name: 'crm'}],
            },
        ],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationCategories.queries', () => ({
    IntegrationCategoryKeys: {integrationCategories: ['integrationCategories']},
    useGetIntegrationCategoriesQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationTags.quries', () => ({
    IntegrationTagKeys: {integrationTags: ['integrationTags']},
    useGetIntegrationTagsQuery: () => ({data: [], error: null, isLoading: false}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    IntegrationWorkflowKeys: {
        integrationWorkflow: (id: number, workflowId: number) => ['integrationWorkflows', id, workflowId],
        integrationWorkflows: (id: number) => ['integrationWorkflows', id],
    },
    useGetIntegrationWorkflowsQuery: () => ({
        data: [{id: '77', integrationWorkflowId: 10, label: 'Welcome flow'}],
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
    WorkflowKeys: {workflow: (id: string) => ['workflow', id]},
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    WorkflowTestConfigurationKeys: {workflowTestConfiguration: (id: string) => ['workflowTestConfiguration', id]},
}));

vi.mock('@/ee/shared/mutations/embedded/integrations.mutations', () => ({
    useDeleteIntegrationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateIntegrationTagsMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useIntegrationWorkflowsByIntegrationIdQuery: () => ({
        data: {integrationWorkflowsByIntegrationId: [{integrationWorkflowId: '10', permissionExpression: 'a == b'}]},
        isFetching: false,
    }),
    useUpdateIntegrationWorkflowPermissionExpressionMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureIntegrationWorkflowCreated: vi.fn(),
        captureIntegrationWorkflowImported: vi.fn(),
    }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header?: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right}: {right?: ReactNode}) => <div>{right}</div>,
}));

vi.mock('@/shared/layout/CategoryTagLeftSidebarNav', () => ({default: () => <nav />}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationDialog', () => ({
    default: () => <div data-testid="integration-dialog" />,
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationPublishDialog', () => ({
    default: () => <div data-testid="publish-dialog" />,
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: ({additionalContent}: {additionalContent?: ReactNode}) => (
        <div data-testid="workflow-dialog">{additionalContent}</div>
    ),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({default: () => <div />}));

vi.mock('@/shared/components/DeleteWorkflowAlertDialog', () => ({default: () => <div />}));

const DEVELOPMENT = 0;
const UNION = -1;

const grant = (development: string[], union: string[] = development) => {
    hoisted.grants = {[DEVELOPMENT]: development, [UNION]: union};
};

const renderPage = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <Integrations />
            </TooltipProvider>
        </MemoryRouter>
    );

const openMenu = async (index = 0) => {
    const menuTriggers = screen
        .getAllByRole('button')
        .filter((button) => button.getAttribute('aria-haspopup') === 'menu' && !button.getAttribute('aria-label'));

    await userEvent.click(menuTriggers[index]);
};

const expandWorkflows = async () => {
    await userEvent.click(screen.getByText('1 workflow'));
};

const openWorkflowEditDialog = async () => {
    await expandWorkflows();

    const menuTriggers = screen.getAllByRole('button', {name: 'More Workflow Actions'});

    await userEvent.click(menuTriggers[menuTriggers.length - 1]);

    await userEvent.click(await screen.findByText('Edit'));
};

describe('Integrations', () => {
    beforeEach(() => {
        hoisted.useRealScopeHook = false;

        grant([]);
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
        });
    });

    it('testAViewerSeesIntegrationsWithoutCreateEditPublishOrDelete', async () => {
        grant(['WORKFLOW_VIEW']);

        renderPage();

        expect(screen.getByText('Mailchimp')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Integration'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Workflow'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add new tag'})).not.toBeInTheDocument();

        await openMenu();

        expect(await screen.findByText('View Workflows')).toBeInTheDocument();
        expect(screen.queryByText('Edit')).not.toBeInTheDocument();
        expect(screen.queryByText('Publish')).not.toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testADevelopmentViewerWhoEditsInProductionCanEditAndPublishButNotCreateWorkflows', async () => {
        grant(['WORKFLOW_VIEW'], ['INTEGRATION_PUBLISH', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        renderPage();

        expect(screen.queryByRole('button', {name: 'Create Workflow'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add new tag'})).toBeInTheDocument();

        await openMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Publish')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testAnEditorBuildsWorkflowsButCannotDeleteTheIntegration', async () => {
        grant(
            ['WORKFLOW_CREATE', 'WORKFLOW_DELETE', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW'],
            ['INTEGRATION_PUBLISH', 'WORKFLOW_CREATE', 'WORKFLOW_DELETE', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']
        );

        renderPage();

        expect(screen.getByRole('button', {name: 'Create Workflow'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add new tag'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Integration'})).not.toBeInTheDocument();

        await openMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Publish')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testOnlyAnAdminEditsThePermissionExpression', async () => {
        grant(['WORKFLOW_EDIT', 'WORKFLOW_VIEW'], ['INTEGRATION_SETTINGS', 'WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        const firstRender = renderPage();

        await openWorkflowEditDialog();

        expect(await screen.findByText('Permission Expression')).toBeInTheDocument();

        firstRender.unmount();

        grant(['WORKFLOW_EDIT', 'WORKFLOW_VIEW'], ['WORKFLOW_EDIT', 'WORKFLOW_VIEW']);

        renderPage();

        await openWorkflowEditDialog();

        expect(await screen.findByTestId('workflow-dialog')).toBeInTheDocument();
        expect(screen.queryByText('Permission Expression')).not.toBeInTheDocument();
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

        expect(screen.getByRole('button', {name: 'Create Integration'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Workflow'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add new tag'})).toBeInTheDocument();

        await openMenu();

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Publish')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });
});
