import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowProjectListItem from '@/ee/pages/embedded/automation-workflows/components/automation-workflow-project-list/AutomationWorkflowProjectListItem';
import {AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];

const handlers = {
    onCreateWorkflow: vi.fn(),
    onDeleteProject: vi.fn(),
    onEditProject: vi.fn(),
    onImportWorkflow: vi.fn(),
    onPublishProject: vi.fn(),
    onSelectWorkflow: vi.fn(),
    onUpdateTags: vi.fn(),
};

const project: AutomationWorkflowProjectType = {
    automationHubVisible: true,
    categoryId: null,
    description: null,
    id: 'project-1',
    lastPublishedVersion: null,
    name: 'Alpha Project',
    permissionExpression: null,
    published: false,
    tagIds: [],
    version: 1,
    workflowTemplates: [
        {
            components: [],
            description: null,
            label: 'Sync Contacts',
            lastModifiedDate: null,
            permissionExpression: null,
            triggers: [],
            workflowId: 'draft-wf-1',
            workflowUuid: 'wf-uuid-1',
        },
        {
            components: [],
            description: null,
            label: 'Send Invoices',
            lastModifiedDate: null,
            permissionExpression: null,
            triggers: [],
            workflowId: 'draft-wf-2',
            workflowUuid: 'wf-uuid-2',
        },
    ],
};

const renderItem = () =>
    render(
        <TooltipProvider>
            <Collapsible>
                <AutomationWorkflowProjectListItem {...handlers} project={project} tags={[]} />
            </Collapsible>
        </TooltipProvider>
    );

// A pointer click on a menu trigger bubbles to the row in jsdom, because jsdom has no hit testing and ignores the
// pointer-events lock Radix puts on the body while a menu is open. The keyboard opens the menu without that click.
const openMenu = async (triggerName = 'More Project Actions') => {
    renderItem();

    screen.getByRole('button', {name: triggerName}).focus();

    await userEvent.keyboard('{ArrowDown}');
};

const getWorkflowsTrigger = () => screen.getByRole('button', {hidden: true, name: /2 workflows/});

describe('AutomationWorkflowProjectListItem', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('project actions menu', () => {
        it('should render with the design-system DropdownMenu', async () => {
            await openMenu();

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Publish Project'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'New Workflow'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Edit Project'})).toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: 'Delete Project'})).toHaveClass(
                'text-content-destructive-primary'
            );
        });

        it('should publish the project when Publish is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Publish Project'}));

            expect(handlers.onPublishProject).toHaveBeenCalledWith('project-1');
        });

        it('should create a workflow when New Workflow is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

            expect(handlers.onCreateWorkflow).toHaveBeenCalledWith('project-1');
        });

        it('should import a workflow when Import Workflow is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

            expect(handlers.onImportWorkflow).toHaveBeenCalledWith('project-1');
        });

        it('should edit the project when Edit is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Edit Project'}));

            expect(handlers.onEditProject).toHaveBeenCalledWith(project);
        });

        it('should ask for confirmation before deleting the project', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Delete Project'}));

            expect(handlers.onDeleteProject).not.toHaveBeenCalled();

            const alertDialog = await screen.findByRole('alertdialog');

            await userEvent.click(within(alertDialog).getByRole('button', {name: 'Delete'}));

            expect(handlers.onDeleteProject).toHaveBeenCalledWith('project-1');
        });
    });

    describe('workflow creation menu', () => {
        it('should render an Import Workflow row', async () => {
            await openMenu('More Workflow Creation Actions');

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Import Workflow'})).toHaveClass('text-content-neutral-primary');
        });

        it('should import a workflow when Import Workflow is clicked', async () => {
            await openMenu('More Workflow Creation Actions');

            await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

            expect(handlers.onImportWorkflow).toHaveBeenCalledWith('project-1');
        });
    });

    describe('row click', () => {
        it('should expand the workflows when the row is clicked', async () => {
            renderItem();

            await userEvent.click(screen.getByText('DRAFT'));

            await waitFor(() => expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'true'));
        });

        it('should keep the workflows collapsed when a menu row is clicked', async () => {
            await openMenu();

            expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');

            await userEvent.click(screen.getByRole('menuitem', {name: 'Publish Project'}));

            expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
        });

        it('should keep the workflows collapsed when the menu itself is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menu'));

            expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
        });

        it('should keep the workflows collapsed when the workflow creation menu itself is clicked', async () => {
            await openMenu('More Workflow Creation Actions');

            await userEvent.click(screen.getByRole('menu'));

            expect(getWorkflowsTrigger()).toHaveAttribute('aria-expanded', 'false');
        });
    });
});
