import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationProjectListItem from '@/ee/pages/embedded/automations/components/automation-project-list/AutomationProjectListItem';
import {AutomationProjectsQuery} from '@/shared/middleware/graphql';
import {
    act,
    render,
    resetAll,
    screen,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];

const mocks = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    deleteMutationOptions: undefined as {onSuccess?: () => void} | undefined,
    deletePending: false,
    duplicateMutate: vi.fn(),
    duplicateMutationOptions: undefined as {onSuccess?: () => void} | undefined,
    invalidateQueries: vi.fn(),
    toast: vi.fn(),
}));

vi.mock('sonner', () => ({toast: mocks.toast}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteAutomationProjectMutation: (options?: {onSuccess?: () => void}) => {
        mocks.deleteMutationOptions = options;

        return {isPending: mocks.deletePending, mutate: mocks.deleteMutate};
    },
    useDuplicateAutomationProjectMutation: (options?: {onSuccess?: () => void}) => {
        mocks.duplicateMutationOptions = options;

        return {isPending: false, mutate: mocks.duplicateMutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: mocks.invalidateQueries}),
}));

const handlers = {
    onCreateConfiguration: vi.fn(),
    onCreateWorkflow: vi.fn(),
    onEditProject: vi.fn(),
    onEditProjectSource: vi.fn(),
    onImportWorkflow: vi.fn(),
    onPublishProject: vi.fn(),
    onSelectWorkflow: vi.fn(),
    onUpdateTags: vi.fn(),
};

const project: AutomationProjectType = {
    categoryId: null,
    codeWorkflowDraftPublishable: null,
    codeWorkflowLanguage: null,
    codeWorkflowProject: false,
    configurationCount: 0,
    description: null,
    id: 'project-1',
    lastPublishedDate: null,
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

const renderItem = (overrides: Partial<AutomationProjectType> = {}) =>
    render(
        <TooltipProvider>
            <Collapsible>
                <AutomationProjectListItem {...handlers} project={{...project, ...overrides}} tags={[]} />
            </Collapsible>
        </TooltipProvider>
    );

// A pointer click on a menu trigger bubbles to the row in jsdom, because jsdom has no hit testing and ignores the
// pointer-events lock Radix puts on the body while a menu is open. The keyboard opens the menu without that click.
const openMenu = async (triggerName = 'More Project Actions', overrides: Partial<AutomationProjectType> = {}) => {
    renderItem(overrides);

    screen.getByRole('button', {name: triggerName}).focus();

    await userEvent.keyboard('{ArrowDown}');
};

const getWorkflowsTrigger = () => screen.getByRole('button', {hidden: true, name: /2 workflows/});

describe('AutomationProjectListItem', () => {
    beforeEach(() => {
        windowResizeObserver();

        mocks.deleteMutationOptions = undefined;
        mocks.deletePending = false;
        mocks.duplicateMutationOptions = undefined;
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

        it('should group the project and workflow actions with Edit first', async () => {
            await openMenu();

            expect(screen.getByRole('group', {name: 'Project Actions'})).toHaveTextContent(
                'EditPublishDuplicateExport'
            );
            expect(screen.getByRole('group', {name: 'Workflow Actions'})).toHaveTextContent(
                'New WorkflowImport WorkflowView Workflows'
            );
        });

        it('should duplicate the project, refresh the list and confirm it when Duplicate is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Duplicate Project'}));

            expect(mocks.duplicateMutate).toHaveBeenCalledWith({id: 'project-1'});

            act(() => {
                mocks.duplicateMutationOptions?.onSuccess?.();
            });

            expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationProjects']});
            expect(mocks.toast).toHaveBeenCalledWith('Project duplicated successfully.');
        });

        it('should open the first workflow in the editor when View Workflows is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'View Workflows'}));

            expect(handlers.onSelectWorkflow).toHaveBeenCalledWith('wf-uuid-1');
        });

        it('should not offer View Workflows when the project has no workflows', async () => {
            await openMenu('More Project Actions', {workflowTemplates: []});

            expect(screen.queryByRole('menuitem', {name: 'View Workflows'})).not.toBeInTheDocument();
        });

        it('should download the project when Export is clicked', async () => {
            const originalLocation = window.location;

            Object.defineProperty(window, 'location', {configurable: true, value: {href: ''}});

            try {
                await openMenu();

                await userEvent.click(screen.getByRole('menuitem', {name: 'Export Project'}));

                expect(window.location.href).toBe('/api/automation/internal/projects/project-1/export');
            } finally {
                Object.defineProperty(window, 'location', {configurable: true, value: originalLocation});
            }
        });

        it('should show the project description in a styled tooltip on the project name', async () => {
            renderItem({description: 'A catalog code project'});

            const projectNameButton = screen.getByRole('button', {name: 'Alpha Project'});

            expect(projectNameButton).not.toHaveAttribute('title');

            await userEvent.hover(projectNameButton);

            expect(await screen.findByRole('tooltip')).toHaveTextContent('A catalog code project');
        });

        it('should publish the project when Publish is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Publish Project'}));

            expect(handlers.onPublishProject).toHaveBeenCalledWith('project-1');
        });

        it('should enable Publish when the project has workflows', async () => {
            await openMenu();

            expect(screen.getByRole('menuitem', {name: 'Publish Project'})).not.toHaveAttribute('data-disabled');
        });

        it('should disable Publish and explain why when the project has no workflows', async () => {
            await openMenu('More Project Actions', {workflowTemplates: []});

            const publishMenuItem = screen.getByRole('menuitem', {name: 'Publish Project'});

            expect(publishMenuItem).toHaveAttribute('data-disabled');

            await userEvent.hover(publishMenuItem.parentElement!);

            expect(await screen.findByRole('tooltip')).toHaveTextContent(
                'Add a workflow before publishing the project.'
            );

            await userEvent.click(publishMenuItem);

            expect(handlers.onPublishProject).not.toHaveBeenCalled();
        });

        it('should create a workflow when New Workflow is clicked', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

            expect(handlers.onCreateWorkflow).toHaveBeenCalledWith('project-1');
        });

        it('should create a configuration when New Configuration is clicked on a published project', async () => {
            await openMenu('More Project Actions', {published: true});

            await userEvent.click(screen.getByRole('menuitem', {name: 'New Configuration'}));

            expect(handlers.onCreateConfiguration).toHaveBeenCalledWith(expect.objectContaining({id: 'project-1'}));
        });

        it('should disable New Configuration until the project is published', async () => {
            await openMenu();

            expect(screen.getByRole('menuitem', {name: 'New Configuration'})).toHaveAttribute('data-disabled');
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

            expect(mocks.deleteMutate).not.toHaveBeenCalled();

            const alertDialog = await screen.findByRole('alertdialog');

            expect(
                within(alertDialog).getByText(
                    'This action cannot be undone. This will permanently delete the project and the workflow templates it contains.'
                )
            ).toBeInTheDocument();

            await userEvent.click(within(alertDialog).getByRole('button', {name: 'Confirm Project Deletion'}));

            expect(mocks.deleteMutate).toHaveBeenCalledWith({id: 'project-1'});
        });

        it('should keep the delete confirmation open until the project is deleted', async () => {
            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Delete Project'}));

            const alertDialog = await screen.findByRole('alertdialog');

            await userEvent.click(within(alertDialog).getByRole('button', {name: 'Confirm Project Deletion'}));

            expect(screen.getByRole('alertdialog')).toBeInTheDocument();

            act(() => {
                mocks.deleteMutationOptions?.onSuccess?.();
            });

            await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

            expect(mocks.invalidateQueries).toHaveBeenCalledWith({queryKey: ['automationProjects']});
        });

        it('should disable the delete confirmation while the project is being deleted', async () => {
            mocks.deletePending = true;

            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Delete Project'}));

            const alertDialog = await screen.findByRole('alertdialog');

            expect(within(alertDialog).getByRole('button', {name: 'Confirm Project Deletion'})).toBeDisabled();
            expect(within(alertDialog).getByRole('button', {name: 'Cancel'})).toBeDisabled();
        });
    });

    describe('status badge', () => {
        it('should show the draft version next to DRAFT on an unpublished project', () => {
            renderItem({published: false, version: 3});

            expect(screen.getByText('DRAFT').parentElement).toHaveTextContent('V3DRAFT');
        });

        it('should show the last published version next to PUBLISHED on a published project', () => {
            renderItem({lastPublishedVersion: 2, published: true, version: 3});

            expect(screen.getByText('PUBLISHED').parentElement).toHaveTextContent('V2PUBLISHED');
        });
    });

    describe('title', () => {
        it('should open the first workflow when the title is clicked', async () => {
            renderItem({description: null});

            await userEvent.click(screen.getByRole('button', {name: project.name}));

            expect(handlers.onSelectWorkflow).toHaveBeenCalledWith('wf-uuid-1');
        });

        it('should underline the title on hover when the project has workflows', () => {
            renderItem({description: null});

            expect(screen.getByRole('button', {name: project.name})).toHaveClass('hover:underline');
        });

        it('should keep a plain title for a project without workflows', () => {
            renderItem({description: null, workflowTemplates: []});

            expect(screen.getByRole('button', {name: project.name})).not.toHaveClass('hover:underline');
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
    });

    describe('summary', () => {
        it('should say when the project was last published', () => {
            const lastPublishedDate = new Date('2026-10-01T10:15:00Z');

            renderItem({lastPublishedDate: lastPublishedDate.toISOString(), lastPublishedVersion: 1, published: true});

            expect(
                screen.getByText(
                    `Published at ${lastPublishedDate.toLocaleDateString()} ${lastPublishedDate.toLocaleTimeString()}`
                )
            ).toBeInTheDocument();
            expect(screen.queryByText('Not yet published')).not.toBeInTheDocument();
        });

        it('should say when the project is not yet published', () => {
            renderItem();

            expect(screen.getByText('Not yet published')).toBeInTheDocument();
        });

        it('should count a single configuration in the singular', () => {
            renderItem({configurationCount: 1});

            expect(screen.getByText('1 configuration')).toBeInTheDocument();
        });

        it('should count several configurations in the plural', () => {
            renderItem({configurationCount: 3});

            expect(screen.getByText('3 configurations')).toBeInTheDocument();
        });
    });
});
