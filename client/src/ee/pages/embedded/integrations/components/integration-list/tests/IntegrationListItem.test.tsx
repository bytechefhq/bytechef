import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationListItem from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem';
import {IntegrationListItemTabType} from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItemTabs';
import {Integration, IntegrationInstanceConfiguration} from '@/ee/shared/middleware/embedded/configuration';
import {fireEvent, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    onTabChange: vi.fn(),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn(), removeQueries: vi.fn()}),
    };
});

vi.mock('react-router-dom', () => ({
    Link: ({children, to}: {children: React.ReactNode; to: string}) => <a href={to}>{children}</a>,
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [new URLSearchParams(''), vi.fn()],
}));

vi.mock('@/ee/shared/mutations/embedded/integrations.mutations', () => ({
    useDeleteIntegrationMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateIntegrationTagsMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useCreateIntegrationWorkflowMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationCategories.queries', () => ({
    IntegrationCategoryKeys: {integrationCategories: ['integrationCategories']},
}));

vi.mock('@/ee/shared/queries/embedded/integrationTags.quries', () => ({
    IntegrationTagKeys: {integrationTags: ['integrationTags']},
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {integration: (id: number) => ['integration', id], integrations: ['integrations']},
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: null}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureIntegrationWorkflowCreated: vi.fn(),
        captureIntegrationWorkflowImported: vi.fn(),
    }),
}));

vi.mock('@/shared/components/TagList', () => ({
    default: () => <div data-testid="tag-list" />,
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: () => <div data-testid="workflow-dialog" />,
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationDialog', () => ({
    default: () => <div data-testid="integration-dialog" />,
}));

vi.mock('@/ee/pages/embedded/integrations/components/IntegrationPublishDialog', () => ({
    default: () => <div data-testid="integration-publish-dialog" />,
}));

vi.mock(
    '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-dialog/IntegrationInstanceConfigurationDialog',
    () => ({
        default: ({
            integrationInstanceConfiguration,
            triggerNode,
        }: {
            integrationInstanceConfiguration: IntegrationInstanceConfiguration;
            triggerNode: React.ReactNode;
        }) => (
            <>
                {triggerNode}

                <span>{`Configuration dialog for integration ${integrationInstanceConfiguration.integrationId} in environment ${integrationInstanceConfiguration.environmentId}`}</span>
            </>
        ),
    })
);

const integration = {
    developmentConfigurationCount: 3,
    id: 1,
    integrationWorkflowIds: [10, 11],
    lastIntegrationVersion: 2,
    lastPublishedDate: new Date(),
    name: 'Mailchimp',
} as Integration;

const renderItem = (overrides: Partial<Integration> = {}, activeTab?: IntegrationListItemTabType) =>
    render(
        <TooltipProvider>
            <Collapsible open={!!activeTab}>
                <IntegrationListItem
                    activeTab={activeTab}
                    configurationsContent={<div>Configurations content</div>}
                    integration={{...integration, ...overrides}}
                    onTabChange={hoisted.onTabChange}
                    workflowsContent={<div>Workflows content</div>}
                />
            </Collapsible>
        </TooltipProvider>
    );

const openMenu = async (triggerName?: string) => {
    renderItem({}, triggerName ? 'workflows' : undefined);

    const menuTriggers = screen
        .getAllByRole('button')
        .filter((button) => button.getAttribute('aria-haspopup') === 'menu');

    const menuTrigger = triggerName
        ? menuTriggers.find((button) => button.getAttribute('aria-label') === triggerName)
        : menuTriggers[menuTriggers.length - 1];

    await userEvent.click(menuTrigger!);
};

describe('IntegrationListItem', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('actions menu', () => {
        it('should render with the design-system DropdownMenu', async () => {
            await openMenu();

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
        });

        it('should open the edit dialog when Edit is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

            expect(screen.getByTestId('integration-dialog')).toBeInTheDocument();
        });

        it('should navigate to the first workflow when View Workflows is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'View Workflows'}));

            expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/1/integration-workflows/10');
        });

        it('should not render View Workflows for an integration without workflows', async () => {
            renderItem({integrationWorkflowIds: []});

            const menuTrigger = screen
                .getAllByRole('button')
                .filter((button) => button.getAttribute('aria-haspopup') === 'menu')
                .pop();

            await userEvent.click(menuTrigger!);

            expect(screen.queryByRole('menuitem', {name: 'View Workflows'})).not.toBeInTheDocument();
        });

        it('should open the publish dialog when Publish is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Publish'}));

            expect(screen.getByTestId('integration-publish-dialog')).toBeInTheDocument();
        });

        it('should ask for confirmation when Delete is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

            expect(screen.getByText(/permanently delete the integration/)).toBeInTheDocument();
        });
    });

    describe('workflow creation menu', () => {
        it('should render an Import Workflow row', async () => {
            await openMenu('More Workflow Creation Actions');

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toHaveClass('text-content-neutral-primary');
        });

        it('should open the file picker when Import Workflow is clicked', async () => {
            await openMenu('More Workflow Creation Actions');

            const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
            const clickSpy = vi.spyOn(fileInput, 'click');

            await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

            expect(clickSpy).toHaveBeenCalled();
        });
    });

    describe('row click', () => {
        it('should open the workflows tab when a collapsed row is clicked', async () => {
            renderItem();

            await userEvent.click(screen.getByText('V1'));

            expect(hoisted.onTabChange).toHaveBeenCalledWith('workflows');
        });

        it('should collapse an open row when it is clicked', async () => {
            renderItem({}, 'workflows');

            fireEvent.click(screen.getByText('V1'));

            expect(hoisted.onTabChange).toHaveBeenCalledWith(undefined);
        });

        it('should keep the row as it is when a menu row is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Publish'}));

            expect(hoisted.onTabChange).not.toHaveBeenCalled();
        });

        it('should keep the row as it is when the menu itself is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menu'));

            expect(hoisted.onTabChange).not.toHaveBeenCalled();
        });
    });

    describe('tabs', () => {
        it('summarizes workflows and configurations and hides the tabs while collapsed', () => {
            renderItem();

            expect(screen.getByText('2 workflows')).toBeInTheDocument();
            expect(screen.getByText('3 configurations')).toBeInTheDocument();
            expect(screen.queryByRole('tab')).not.toBeInTheDocument();
            expect(screen.queryByLabelText('Create Workflow')).not.toBeInTheDocument();
        });

        it('shows configurations without a number when the server did not send a count', () => {
            renderItem({developmentConfigurationCount: undefined});

            expect(screen.getByText('configurations')).toBeInTheDocument();
            expect(screen.queryByText('0 configurations')).not.toBeInTheDocument();
        });

        it('shows one configuration in the singular', () => {
            renderItem({developmentConfigurationCount: 1});

            expect(screen.getByText('1 configuration')).toBeInTheDocument();
        });

        it('shows the tabs and the active tab content when expanded', () => {
            renderItem({}, 'configurations');

            expect(screen.getByRole('tab', {name: 'Configurations'})).toHaveAttribute('aria-selected', 'true');
            expect(screen.getByText('Configurations content')).toBeInTheDocument();
        });

        it('switches tabs', async () => {
            renderItem({}, 'workflows');

            await userEvent.click(screen.getByRole('tab', {name: 'Configurations'}));

            expect(hoisted.onTabChange).toHaveBeenCalledWith('configurations');
        });

        it('offers New Workflow on the workflows tab', () => {
            renderItem({}, 'workflows');

            expect(screen.getByLabelText('Create Workflow')).toHaveTextContent('New Workflow');
            expect(screen.queryByLabelText('Create Configuration')).not.toBeInTheDocument();
        });

        it('offers a Development New Configuration for the integration on the configurations tab', () => {
            renderItem({}, 'configurations');

            expect(screen.getByLabelText('Create Configuration')).toBeEnabled();
            expect(screen.getByLabelText('Create Configuration')).toHaveTextContent('New Configuration');
            expect(screen.getByText('Configuration dialog for integration 1 in environment 0')).toBeInTheDocument();
            expect(screen.queryByLabelText('Create Workflow')).not.toBeInTheDocument();
        });

        it('disables New Configuration until the integration is published', async () => {
            renderItem({lastPublishedDate: undefined}, 'configurations');

            expect(screen.getByLabelText('Create Configuration')).toBeDisabled();
            expect(screen.queryByText(/Configuration dialog/)).not.toBeInTheDocument();

            await userEvent.hover(screen.getByLabelText('Create Configuration').parentElement!);

            expect((await screen.findAllByText('Publish the integration first'))[0]).toBeInTheDocument();
        });
    });
});
