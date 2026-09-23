import {TooltipProvider} from '@/components/ui/tooltip';
import AutomationWorkflowEditorLeftSidebar from '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/AutomationWorkflowEditorLeftSidebar';
import {fireEvent, render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    projectsQuery: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/components/ui/scroll-area', () => ({
    ScrollArea: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorProjectSelect',
    () => ({
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
    })
);

vi.mock(
    '@/ee/pages/embedded/automation-workflow/components/automation-workflow-editor/components/AutomationWorkflowEditorWorkflowsListItemDropdownMenu',
    () => ({
        default: () => null,
    })
);

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationWorkflowProjectsQuery: () => hoisted.projectsQuery(),
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
        automationHubVisible: true,
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
        automationHubVisible: true,
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
            <AutomationWorkflowEditorLeftSidebar currentWorkflowId={currentWorkflowId} />
        </TooltipProvider>
    );

const getWorkflowLabels = () =>
    Array.from(document.querySelectorAll('li span.text-sm.font-medium')).map(
        (workflowLabel) => workflowLabel.textContent
    );

beforeEach(() => {
    vi.clearAllMocks();

    hoisted.projectsQuery.mockReturnValue({
        data: {automationWorkflowProjects: mockProjects},
        isLoading: false,
    });
});

it('selects the project of the current workflow', () => {
    renderLeftSidebar();

    expect(screen.getByText('Selected:project-1')).toBeInTheDocument();
});

it("lists the current project's workflows, last edited first", () => {
    renderLeftSidebar();

    expect(getWorkflowLabels()).toEqual(['Bouncer', 'Mailer']);
    expect(screen.queryByText('Reporter')).not.toBeInTheDocument();
});

it('no longer offers project or workflow creation in the sidebar', () => {
    renderLeftSidebar();

    expect(screen.queryByRole('button', {name: /new project/i})).not.toBeInTheDocument();
    expect(screen.queryByRole('button', {name: /new workflow/i})).not.toBeInTheDocument();
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

    expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows/workflow-2/editor');
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
});
