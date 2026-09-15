import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import React from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectsLeftSidebar from './ProjectsLeftSidebar';

// React Query test client setup
const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

let queryClient: QueryClient;

// Mocks for UI components used inside dropdown/scroll
vi.mock('@/components/Button/Button', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({children, icon, label, ...props}: any) => (
        <button data-testid="btn" {...props}>
            {icon}

            {label ?? children}
        </button>
    ),
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenu: ({children}: any) => <div>{children}</div>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenuContent: ({children}: any) => <div>{children}</div>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenuItem: ({children, disabled, onClick}: any) => (
        <div aria-disabled={disabled ? 'true' : undefined} onClick={disabled ? undefined : onClick} role="menuitem">
            {children}
        </div>
    ),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenuTrigger: ({children}: any) => <div>{children}</div>,
}));

vi.mock('@/components/ui/scroll-area', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ScrollArea: ({children, ...props}: any) => <div {...props}>{children}</div>,
}));

// Child components mocked to minimal renderers
vi.mock('@/pages/automation/project/components/projects-sidebar/components/ProjectSelect', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({selectedProjectId}: any) => <div data-testid="project-select">ProjectSelect:{selectedProjectId}</div>,
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/components/ProjectWorkflowsList', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({project}: any) => <div data-testid="project-workflows-list">Project:{project.id}</div>,
}));

vi.mock('@/shared/components/workflow/WorkflowsListFilter', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({sortBy}: any) => <div data-testid="workflows-list-filter">Sort:{sortBy}</div>,
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/components/WorkflowsListItem', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({workflow}: any) => <li data-testid="workflow-item">Workflow:{workflow.id}</li>,
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/components/WorkflowsListSkeleton', () => ({
    default: () => <div data-testid="skeleton">Loading...</div>,
}));

vi.mock('@/pages/automation/projects/components/ProjectDialog', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({onClose}: any) => (
        <div role="dialog">
            <span>ProjectDialog</span>

            <button onClick={onClose}>Close dialog</button>
        </div>
    ),
}));

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({onClose, parentId}: any) => (
        <div role="dialog">
            <span>WorkflowDialog:{parentId}</span>

            <button onClick={onClose}>Close dialog</button>
        </div>
    ),
}));

// Hooks and stores
const mockUseCreateProjectWorkflow = vi.hoisted(() => vi.fn());
vi.mock('@/pages/automation/project/hooks/useCreateProjectWorkflow', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useCreateProjectWorkflow: (args: any) => mockUseCreateProjectWorkflow(args),
}));

const mockUseImportProjectWorkflow = vi.hoisted(() => vi.fn());
vi.mock('@/pages/automation/project/hooks/useImportProjectWorkflow', () => ({
    useImportProjectWorkflow: (projectId: number) => mockUseImportProjectWorkflow(projectId),
}));

const mockCaptureProjectImported = vi.hoisted(() => vi.fn());
vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectImported: mockCaptureProjectImported}),
}));

const mockImportProjectMutate = vi.hoisted(() => vi.fn());
vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useImportProjectMutation: (options: any) => ({
        mutate: (request: unknown) => {
            mockImportProjectMutate(request);

            options?.onSuccess?.();
        },
    }),
}));

const mockToast = vi.hoisted(() => vi.fn());
vi.mock('sonner', () => ({toast: mockToast}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: vi.fn(),
}));

const mockGetProjectWorkflowsQuery = vi.fn();
const mockGetWorkspaceWorkflowsQuery = vi.fn();
vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useGetProjectWorkflowsQuery: (...args: any[]) => mockGetProjectWorkflowsQuery(...args),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useGetWorkspaceWorkflowsQuery: (...args: any[]) => mockGetWorkspaceWorkflowsQuery(...args),
}));

const mockGetWorkspaceProjectsQuery = vi.fn();
vi.mock('@/shared/queries/automation/projects.queries', async () => ({
    ProjectKeys: {project: (id: number) => ['project', id], projects: ['projects']},
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useGetWorkspaceProjectsQuery: (args: any) => mockGetWorkspaceProjectsQuery(args),
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar', () => ({
    useProjectsLeftSidebar: () => ({
        calculateTimeDifference: vi.fn(),
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        getFilteredWorkflows: (workflows: any[]) => workflows || [],
        getWorkflowsProjectId: () => vi.fn(),
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 10}),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');
    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

const mockNavigate = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async () => ({
    useNavigate: () => mockNavigate,
}));

const setupImportProjectWorkflow = ({importN8nWorkflowDisabled = false} = {}) =>
    mockUseImportProjectWorkflow.mockReturnValue({
        handleN8nWorkflowFileChange: vi.fn(),
        handleWorkflowFileChange: vi.fn(),
        importN8nWorkflowDisabled,
        isImportingN8nWorkflow: false,
        n8nWorkflowFileInputRef: {current: null},
        workflowFileInputRef: {current: null},
    });

const fileInputByAccept = (accept: string) =>
    document.querySelector(`input[type="file"][accept="${accept}"]`) as HTMLInputElement;

// Helper to set default mocks per test scenario
const setupQueries = ({
    loading = false,
    projects = [{id: 1}, {id: 2}],
    selectedProjectId,
    workflows = [{id: 'w1'}, {id: 'w2'}],
}: {
    selectedProjectId: number;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    projects?: any[];
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    workflows?: any[];
    loading?: boolean;
}) => {
    mockGetWorkspaceProjectsQuery.mockReturnValue({data: projects, refetch: vi.fn()});

    if (selectedProjectId !== 0) {
        mockGetProjectWorkflowsQuery.mockReturnValue({data: workflows, isLoading: loading});
        mockGetWorkspaceWorkflowsQuery.mockReturnValue({data: undefined, isLoading: false});
    } else {
        mockGetProjectWorkflowsQuery.mockReturnValue({data: undefined, isLoading: false});
        mockGetWorkspaceWorkflowsQuery.mockReturnValue({data: workflows, isLoading: loading});
    }
};

const baseProps = {
    currentWorkflowId: 'w1',
    onProjectClick: vi.fn(),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    updateWorkflowMutation: {} as any,
};

// Helper render wrapper to provide required Providers (React Query + Tooltip)
const renderWithProviders = (ui: React.ReactElement) =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );

describe('ProjectsLeftSidebar', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        queryClient = createTestQueryClient();

        setupImportProjectWorkflow();
    });

    afterEach(() => {
        queryClient.clear();
    });

    it('shows loading skeleton when queries are loading', async () => {
        setupQueries({loading: true, selectedProjectId: 5});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={5} />);

        // isLoading becomes true after effect runs
        await waitFor(() => expect(screen.getByTestId('skeleton')).toBeInTheDocument());
    });

    it('renders ProjectWorkflowsList for each project when selectedProjectId is 0', async () => {
        const projects = [{id: 11}, {id: 22}, {id: 33}];
        setupQueries({projects, selectedProjectId: 0, workflows: [{id: 'wa'}]});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={0} />);

        // Lists per project
        const items = await screen.findAllByTestId('project-workflows-list');
        expect(items).toHaveLength(projects.length);
    });

    it('renders WorkflowsListItem for each workflow when a specific project is selected', async () => {
        const workflows = [{id: 'wa'}, {id: 'wb'}, {id: 'wc'}];
        setupQueries({selectedProjectId: 7, workflows});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={7} />);

        const items = await screen.findAllByTestId('workflow-item');
        expect(items).toHaveLength(workflows.length);
    });

    it('shows the Workflow button and opens WorkflowDialog for the selected project on click', async () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        fireEvent.click(screen.getByText('New Workflow'));

        expect(await screen.findByRole('dialog')).toHaveTextContent('WorkflowDialog:9');
        expect(mockUseCreateProjectWorkflow).toHaveBeenCalledWith(expect.objectContaining({projectId: 9}));
        expect(mockUseImportProjectWorkflow).toHaveBeenCalledWith(9);
    });

    it('creates workflows in the current project when all projects are listed', async () => {
        setupQueries({selectedProjectId: 0});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={0} />);

        fireEvent.click(screen.getByText('New Workflow'));

        expect(await screen.findByRole('dialog')).toHaveTextContent('WorkflowDialog:0');
    });

    it('opens the ProjectDialog from the New Project menu', async () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        expect(screen.getByLabelText('New Project')).toBeInTheDocument();

        fireEvent.click(screen.getByText('From Scratch'));

        expect(await screen.findByRole('dialog')).toHaveTextContent('ProjectDialog');
    });

    it('renders hidden file inputs for workflow, n8n workflow and project imports', () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        const acceptValues = Array.from(document.querySelectorAll('input[type="file"]')).map((input) =>
            input.getAttribute('accept')
        );

        expect(acceptValues).toEqual(['.json,.yaml,.yml', '.json', '.zip']);
    });

    it('closes the WorkflowDialog and the ProjectDialog', async () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        fireEvent.click(screen.getByText('New Workflow'));
        fireEvent.click(await screen.findByText('Close dialog'));

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        fireEvent.click(screen.getByText('From Scratch'));
        fireEvent.click(await screen.findByText('Close dialog'));

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('opens the matching file picker from the import menu items', () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        const workflowInputClick = vi.spyOn(fileInputByAccept('.json,.yaml,.yml'), 'click');
        const n8nWorkflowInputClick = vi.spyOn(fileInputByAccept('.json'), 'click');
        const projectInputClick = vi.spyOn(fileInputByAccept('.zip'), 'click');

        fireEvent.click(screen.getByText('Import Workflow'));
        fireEvent.click(screen.getByText('Import n8n Workflow'));
        fireEvent.click(screen.getByText('Import Project'));

        expect(workflowInputClick).toHaveBeenCalledTimes(1);
        expect(n8nWorkflowInputClick).toHaveBeenCalledTimes(1);
        expect(projectInputClick).toHaveBeenCalledTimes(1);
    });

    it('imports the selected project file into the current workspace', () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        const file = new File(['zip'], 'project.zip', {type: 'application/zip'});

        fireEvent.change(fileInputByAccept('.zip'), {target: {files: [file]}});

        expect(mockImportProjectMutate).toHaveBeenCalledWith({file, workspaceId: 10});
        expect(mockCaptureProjectImported).toHaveBeenCalledTimes(1);
        expect(mockToast).toHaveBeenCalledWith('Project is imported.');
    });

    it('disables the Import n8n Workflow item when no AI provider is enabled', async () => {
        setupImportProjectWorkflow({importN8nWorkflowDisabled: true});
        setupQueries({selectedProjectId: 5});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={5} />);

        const menuItem = (await screen.findByText('Import n8n Workflow')).closest('[role="menuitem"]');

        expect(menuItem).toHaveAttribute('aria-disabled', 'true');
    });

    it('opens the template pages with absolute routes', () => {
        setupQueries({selectedProjectId: 5});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={5} />);

        const templateItems = screen
            .getAllByRole('menuitem')
            .filter((menuItem) => /From Template/.test(menuItem.textContent ?? ''));

        expect(templateItems).toHaveLength(2);

        fireEvent.click(templateItems[0]);

        expect(mockNavigate).toHaveBeenCalledWith('/automation/projects/templates');

        fireEvent.click(templateItems[1]);

        expect(mockNavigate).toHaveBeenCalledWith('/automation/projects/5/templates');
    });

    it('updates selectedProjectId when projectId prop changes', async () => {
        const workflows1 = [{id: 'wa'}, {id: 'wb'}];
        const workflows2 = [{id: 'wc'}, {id: 'wd'}];

        // Initially render with projectId = 5
        setupQueries({selectedProjectId: 5, workflows: workflows1});

        const {rerender} = renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={5} />);

        // Verify initial workflows are displayed
        await waitFor(() => {
            const items = screen.getAllByTestId('workflow-item');
            expect(items).toHaveLength(2);
            expect(screen.getByText('Workflow:wa')).toBeInTheDocument();
        });

        // Verify useGetProjectWorkflowsQuery was called with projectId = 5
        expect(mockGetProjectWorkflowsQuery).toHaveBeenCalledWith(5, true);

        // Now change projectId to 7
        setupQueries({selectedProjectId: 7, workflows: workflows2});

        rerender(
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <ProjectsLeftSidebar {...baseProps} projectId={7} />
                </TooltipProvider>
            </QueryClientProvider>
        );

        // Verify the query was called with the new projectId
        await waitFor(() => {
            expect(mockGetProjectWorkflowsQuery).toHaveBeenCalledWith(7, true);
        });

        // Verify new workflows are displayed
        await waitFor(() => {
            const items = screen.getAllByTestId('workflow-item');
            expect(items).toHaveLength(2);
            expect(screen.getByText('Workflow:wc')).toBeInTheDocument();
        });
    });

    it('handles NaN projectId by defaulting to 0 (All projects)', async () => {
        const projects = [{id: 11}, {id: 22}];
        setupQueries({projects, selectedProjectId: 0, workflows: [{id: 'wa'}]});

        // Pass NaN as projectId (simulating parseInt(undefined))
        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={NaN} />);

        // Should show ProjectWorkflowsList for all projects (selectedProjectId = 0)
        await waitFor(() => {
            expect(screen.getByTestId('project-select')).toHaveTextContent('ProjectSelect:0');
        });

        const items = await screen.findAllByTestId('project-workflows-list');
        expect(items).toHaveLength(projects.length);

        // Verify the "all workflows" query was called with enabled=true
        expect(mockGetWorkspaceWorkflowsQuery).toHaveBeenCalledWith(10, true);
        // And the project-specific query was disabled (NaN is converted to 0)
        expect(mockGetProjectWorkflowsQuery).toHaveBeenCalledWith(0, false);
    });

    it('updates from NaN to valid projectId when prop changes after initial load', async () => {
        const projects = [{id: 11}, {id: 22}];
        const workflows = [{id: 'wa'}, {id: 'wb'}];

        // Start with NaN projectId (initial state after login before URL params are parsed)
        setupQueries({projects, selectedProjectId: 0, workflows: [{id: 'initial'}]});

        const {rerender} = renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={NaN} />);

        // Initially should show all projects
        await waitFor(() => {
            expect(screen.getByTestId('project-select')).toHaveTextContent('ProjectSelect:0');
        });

        // Now update to a valid projectId (simulating URL params being parsed)
        setupQueries({selectedProjectId: 5, workflows});

        rerender(
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <ProjectsLeftSidebar {...baseProps} projectId={5} />
                </TooltipProvider>
            </QueryClientProvider>
        );

        // Should now show the specific project
        await waitFor(() => {
            expect(screen.getByTestId('project-select')).toHaveTextContent('ProjectSelect:5');
        });

        // Verify the project-specific query was called with the valid projectId
        await waitFor(() => {
            expect(mockGetProjectWorkflowsQuery).toHaveBeenCalledWith(5, true);
        });

        // Verify workflows for the specific project are displayed
        const items = await screen.findAllByTestId('workflow-item');
        expect(items).toHaveLength(workflows.length);
    });

    it('uses a flex-bounded scroll container so the last workflow stays reachable (regression for #5111)', async () => {
        setupQueries({selectedProjectId: 7, workflows: [{id: 'wa'}, {id: 'wb'}, {id: 'wc'}]});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={7} />);

        const items = await screen.findAllByTestId('workflow-item');
        const scrollContainer = items[0].closest('ul')?.parentElement as HTMLElement;

        // The scroll container must fill the remaining flex space and be allowed to shrink
        // (flex-1 + min-h-0) rather than be pinned to the viewport height (h-screen), which
        // pushed its bottom below the fold and clipped the last workflow.
        expect(scrollContainer.className).toContain('flex-1');
        expect(scrollContainer.className).toContain('min-h-0');
        expect(scrollContainer.className).not.toContain('h-screen');
    });

    it('handles zero projectId correctly (should show all projects)', async () => {
        const projects = [{id: 11}, {id: 22}];
        setupQueries({projects, selectedProjectId: 0, workflows: [{id: 'wa'}]});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={0} />);

        // Should show all projects
        await waitFor(() => {
            expect(screen.getByTestId('project-select')).toHaveTextContent('ProjectSelect:0');
        });

        const items = await screen.findAllByTestId('project-workflows-list');
        expect(items).toHaveLength(projects.length);
    });
});
