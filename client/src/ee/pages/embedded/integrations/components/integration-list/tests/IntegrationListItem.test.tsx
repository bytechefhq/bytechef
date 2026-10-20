import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationListItem from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem';
import {Integration} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
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

const integration = {
    id: 1,
    integrationWorkflowIds: [10, 11],
    lastIntegrationVersion: 2,
    name: 'Mailchimp',
} as Integration;

const renderItem = (overrides: Partial<Integration> = {}) =>
    render(
        <TooltipProvider>
            <Collapsible>
                <IntegrationListItem integration={{...integration, ...overrides}} />
            </Collapsible>
        </TooltipProvider>
    );

const openMenu = async (triggerName?: string) => {
    renderItem();

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
        it('should expand the workflows when the row is clicked', async () => {
            renderItem();

            await userEvent.click(screen.getByText('V2'));

            expect(screen.getByRole('button', {hidden: true, name: /2 workflows/})).toHaveAttribute(
                'aria-expanded',
                'true'
            );
        });

        it('should keep the workflows collapsed when a menu row is clicked', async () => {
            await openMenu();

            const workflowsTrigger = screen.getByRole('button', {hidden: true, name: /2 workflows/});

            expect(workflowsTrigger).toHaveAttribute('aria-expanded', 'false');

            await userEvent.click(screen.getByRole('menuitem', {name: 'Publish'}));

            expect(workflowsTrigger).toHaveAttribute('aria-expanded', 'false');
        });

        it('should keep the workflows collapsed when the menu itself is clicked', async () => {
            await openMenu();

            const workflowsTrigger = screen.getByRole('button', {hidden: true, name: /2 workflows/});

            await userEvent.click(screen.getByRole('menu'));

            expect(workflowsTrigger).toHaveAttribute('aria-expanded', 'false');
        });
    });
});
