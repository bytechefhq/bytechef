import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationEditorLeftSidebar from '@/ee/pages/embedded/automation/components/automation-editor/AutomationEditorLeftSidebar';
import {useAutomationEditorSidebarStore} from '@/ee/pages/embedded/automation/components/automation-editor/stores/useAutomationEditorSidebarStore';
import {fireEvent, render, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    createProjectMutate: vi.fn(),
    createWorkflow: vi.fn(),
    navigate: vi.fn(),
    projectsQuery: vi.fn(),
    useCreateAutomationProjectWorkflow: vi.fn(),
    workflowFileInputClick: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/components/ui/scroll-area', () => ({
    ScrollArea: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorProjectSelect', () => ({
    ALL_PROJECTS_VALUE: '0',
    default: ({
        selectedProjectId,
        setSelectedProjectId,
    }: {
        selectedProjectId: string;
        setSelectedProjectId: (projectId: string) => void;
    }) => (
        <div data-testid="project-select">
            <span>Selected:{selectedProjectId}</span>

            <button onClick={() => setSelectedProjectId('0')}>All projects</button>

            <button onClick={() => setSelectedProjectId('project-2')}>Reporting project</button>
        </div>
    ),
}));

vi.mock(
    '@/ee/pages/embedded/automation/components/automation-editor/components/AutomationEditorWorkflowsListItemDropdownMenu',
    () => ({
        default: () => null,
    })
);

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationProjectCategoriesQuery: () => ({data: {automationProjectCategories: []}}),
    useAutomationProjectTagsQuery: () => ({data: {automationProjectTags: []}}),
    useAutomationProjectsQuery: () => hoisted.projectsQuery(),
    useCreateAutomationProjectMutation: () => ({
        mutate: (variables: unknown, options?: {onSuccess?: (data: {createAutomationProject: string}) => void}) => {
            hoisted.createProjectMutate(variables);

            options?.onSuccess?.({createAutomationProject: 'project-2'});
        },
    }),
}));

vi.mock('@/ee/pages/embedded/automation/hooks/useCreateAutomationProjectWorkflow', () => ({
    useCreateAutomationProjectWorkflow: (props: unknown) => {
        hoisted.useCreateAutomationProjectWorkflow(props);

        return {
            createWorkflow: hoisted.createWorkflow,
            handleWorkflowFileChange: vi.fn(),
            workflowFileInputRef: {
                get current() {
                    return {click: hoisted.workflowFileInputClick};
                },
                set current(_value) {},
            },
        };
    },
}));

vi.mock('@/ee/pages/embedded/automations/components/automation-dialog/AutomationDialog', () => ({
    default: ({onSubmit}: {onSubmit: (values: {description: string; label: string}) => void}) => (
        <div>
            <span>Workflow dialog</span>

            <button onClick={() => onSubmit({description: '', label: 'New workflow'})} type="button">
                Submit workflow
            </button>
        </div>
    ),
}));

vi.mock('@/ee/pages/embedded/automations/components/automation-project-dialog/AutomationProjectDialog', () => ({
    default: ({onSubmit}: {onSubmit: (values: Record<string, unknown>) => void}) => (
        <div>
            <span>Project dialog</span>

            <button
                onClick={() =>
                    onSubmit({
                        category: '',
                        description: '',
                        name: 'New project',
                        permissionExpression: '',
                        tags: [],
                    })
                }
                type="button"
            >
                Submit project
            </button>
        </div>
    ),
}));

const createWorkflowTemplate = (workflowUuid: string, label: string, lastModifiedDate: string) => ({
    components: [],
    description: null,
    label,
    lastModifiedDate,
    permissionExpression: null,
    triggers: [],
    workflowId: `draft-${workflowUuid}`,
    workflowUuid,
});

const mockProjects = [
    {
        categoryId: null,
        description: null,
        id: 'project-1',
        lastPublishedVersion: null,
        name: 'Mailing',
        permissionExpression: null,
        published: false,
        tagIds: [],
        version: 1,
        workflowTemplates: [
            createWorkflowTemplate('workflow-1', 'Mailer', '2026-01-01T10:00:00Z'),
            createWorkflowTemplate('workflow-2', 'Bouncer', '2026-01-02T10:00:00Z'),
        ],
    },
    {
        categoryId: null,
        description: null,
        id: 'project-2',
        lastPublishedVersion: null,
        name: 'Reporting',
        permissionExpression: null,
        published: false,
        tagIds: [],
        version: 1,
        workflowTemplates: [createWorkflowTemplate('workflow-3', 'Reporter', '2026-01-03T10:00:00Z')],
    },
];

const renderLeftSidebar = (currentWorkflowId = 'workflow-1') =>
    render(
        <TooltipProvider>
            <AutomationEditorLeftSidebar currentWorkflowId={currentWorkflowId} />
        </TooltipProvider>
    );

const getWorkflowLabels = () =>
    Array.from(document.querySelectorAll('li span.text-sm.font-medium')).map(
        (workflowLabel) => workflowLabel.textContent
    );

beforeEach(() => {
    vi.clearAllMocks();

    windowResizeObserver();

    useAutomationEditorSidebarStore.setState({leftSidebarOpen: false});

    hoisted.projectsQuery.mockReturnValue({
        data: {automationProjects: mockProjects},
        isLoading: false,
    });
});

it('selects the project of the current workflow', () => {
    renderLeftSidebar();

    expect(screen.getByText('Selected:project-1')).toBeInTheDocument();
});

it('selects the new current project when the current workflow moves to another project', () => {
    const {rerender} = renderLeftSidebar();

    expect(screen.getByText('Selected:project-1')).toBeInTheDocument();

    rerender(
        <TooltipProvider>
            <AutomationEditorLeftSidebar currentWorkflowId="workflow-3" />
        </TooltipProvider>
    );

    expect(screen.getByText('Selected:project-2')).toBeInTheDocument();
    expect(getWorkflowLabels()).toEqual(['Reporter']);
});

it('keeps the selected project when switching to another workflow of the same project', async () => {
    const {rerender} = renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'All projects'}));

    rerender(
        <TooltipProvider>
            <AutomationEditorLeftSidebar currentWorkflowId="workflow-2" />
        </TooltipProvider>
    );

    expect(screen.getByText('Selected:0')).toBeInTheDocument();
});

it("lists the current project's workflows, last edited first", () => {
    renderLeftSidebar();

    expect(getWorkflowLabels()).toEqual(['Bouncer', 'Mailer']);
    expect(screen.queryByText('Reporter')).not.toBeInTheDocument();
});

it('creates a project from the New Project button and selects it', async () => {
    renderLeftSidebar();

    await userEvent.hover(screen.getByRole('button', {name: 'New Project'}));

    expect(await screen.findByRole('tooltip')).toHaveTextContent('New Project');

    await userEvent.click(screen.getByRole('button', {name: 'New Project'}));
    await userEvent.click(screen.getByRole('button', {name: 'Submit project'}));

    expect(hoisted.createProjectMutate).toHaveBeenCalledWith(
        expect.objectContaining({name: 'New project', permissionExpression: ''})
    );
    expect(screen.queryByText('Project dialog')).not.toBeInTheDocument();
    expect(screen.getByText('Selected:project-2')).toBeInTheDocument();
});

it('creates a workflow in the current project from the New Workflow button', async () => {
    renderLeftSidebar();

    expect(hoisted.useCreateAutomationProjectWorkflow).toHaveBeenLastCalledWith(
        expect.objectContaining({projectId: 'project-1'})
    );

    await userEvent.click(screen.getByRole('button', {name: 'New Workflow'}));
    await userEvent.click(screen.getByRole('button', {name: 'Submit workflow'}));

    expect(hoisted.createWorkflow).toHaveBeenCalledWith({description: '', label: 'New workflow'});
    expect(screen.queryByText('Workflow dialog')).not.toBeInTheDocument();
});

it('creates workflows in the selected project', async () => {
    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'Reporting project'}));

    expect(hoisted.useCreateAutomationProjectWorkflow).toHaveBeenLastCalledWith(
        expect.objectContaining({projectId: 'project-2'})
    );
});

it('creates workflows in the current project when all projects are selected', async () => {
    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'All projects'}));

    expect(hoisted.useCreateAutomationProjectWorkflow).toHaveBeenLastCalledWith(
        expect.objectContaining({projectId: 'project-1'})
    );
});

it('imports a workflow from the workflow options menu', async () => {
    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'More workflow options'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

    expect(hoisted.workflowFileInputClick).toHaveBeenCalledTimes(1);
});

it('focuses the search input once the open sidebar has loaded', async () => {
    useAutomationEditorSidebarStore.setState({leftSidebarOpen: true});

    renderLeftSidebar();

    await waitFor(
        () => {
            expect(screen.getByRole('textbox')).toHaveFocus();
        },
        {interval: 5, timeout: 5000}
    );
});

it('shows the version and status of each project when all projects are selected', async () => {
    hoisted.projectsQuery.mockReturnValue({
        data: {
            automationProjects: [
                {...mockProjects[0], lastPublishedVersion: 2, version: 2},
                {...mockProjects[1], lastPublishedVersion: 1, version: 2},
            ],
        },
        isLoading: false,
    });

    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'All projects'}));

    const [mailingHeader, reportingHeader] = screen
        .getAllByRole('heading')
        .map((heading) => heading.parentElement!.textContent);

    expect(mailingHeader).toBe('MailingV2PUBLISHED');
    expect(reportingHeader).toBe('ReportingV2DRAFT');
});

it('lists the workflows of another selected project', async () => {
    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'Reporting project'}));

    expect(getWorkflowLabels()).toEqual(['Reporter']);
});

it('groups the workflows of every project under its name when all projects are selected', async () => {
    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'All projects'}));

    expect(screen.getByRole('heading', {name: 'Mailing'})).toBeInTheDocument();
    expect(screen.getByRole('heading', {name: 'Reporting'})).toBeInTheDocument();
    expect(getWorkflowLabels()).toEqual(['Bouncer', 'Mailer', 'Reporter']);
});

it('filters the workflows by the search text', () => {
    renderLeftSidebar();

    fireEvent.change(screen.getByRole('textbox'), {target: {value: 'mail'}});

    expect(getWorkflowLabels()).toEqual(['Mailer']);
});

it('sorts the workflows by the chosen order', async () => {
    renderLeftSidebar();

    await userEvent.click(screen.getByRole('button', {name: 'Sort by'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'A-Z'}));

    expect(getWorkflowLabels()).toEqual(['Bouncer', 'Mailer']);

    await userEvent.click(screen.getByRole('button', {name: 'Sort by'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Z-A'}));

    expect(getWorkflowLabels()).toEqual(['Mailer', 'Bouncer']);
});

it('reports when no workflow matches the search text', () => {
    renderLeftSidebar();

    fireEvent.change(screen.getByRole('textbox'), {target: {value: 'nothing matches'}});

    expect(screen.getByText('No workflows found')).toBeInTheDocument();
});

it('navigates to another workflow when its item is clicked', () => {
    renderLeftSidebar();

    fireEvent.click(screen.getByText('Bouncer'));

    expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor');
});

it('does not navigate when the current workflow is clicked', () => {
    renderLeftSidebar();

    fireEvent.click(screen.getByText('Mailer'));

    expect(hoisted.navigate).not.toHaveBeenCalled();
});

it('shows skeletons instead of the project select while the projects load', () => {
    hoisted.projectsQuery.mockReturnValue({data: undefined, isLoading: true});

    renderLeftSidebar();

    expect(screen.queryByTestId('project-select')).not.toBeInTheDocument();
    expect(screen.queryByText('No workflows found')).not.toBeInTheDocument();
    expect(document.querySelectorAll('.rounded-full')).toHaveLength(4);
});
