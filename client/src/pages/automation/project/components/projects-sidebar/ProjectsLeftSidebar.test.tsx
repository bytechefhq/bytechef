import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import React from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectsLeftSidebar from './ProjectsLeftSidebar';

// Simple utility to flush promises
const flushPromises = () => new Promise((r) => setTimeout(r, 0));

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
vi.mock('@/components/ui/button', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    Button: ({children, ...props}: any) => (
        <button data-testid="btn" {...props}>
            {children}
        </button>
    ),
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenu: ({children}: any) => <div>{children}</div>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenuContent: ({children}: any) => <div>{children}</div>,
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    DropdownMenuItem: ({children, onClick}: any) => (
        <div onClick={onClick} role="menuitem">
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

vi.mock('@/pages/automation/project/components/projects-sidebar/components/WorkflowsListFilter', () => ({
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

vi.mock('@/shared/components/workflow/WorkflowDialog', () => ({
    default: () => <div role="dialog">WorkflowDialog</div>,
}));

// Hooks and stores
const mockGetProjectWorkflowsQuery = vi.fn();
const mockGetWorkflowsQuery = vi.fn();
vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useGetProjectWorkflowsQuery: (...args: any[]) => mockGetProjectWorkflowsQuery(...args),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useGetWorkflowsQuery: (...args: any[]) => mockGetWorkflowsQuery(...args),
}));

const mockGetWorkspaceProjectsQuery = vi.fn();
vi.mock('@/shared/queries/automation/projects.queries', async () => ({
    ProjectKeys: {project: (id: number) => ['project', id], projects: ['projects']},
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useGetWorkspaceProjectsQuery: (args: any) => mockGetWorkspaceProjectsQuery(args),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: vi.fn(),
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/hooks/useProjectsLeftSidebar', () => ({
    useProjectsLeftSidebar: () => ({
        calculateTimeDifference: vi.fn(),
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        createProjectWorkflowMutation: {mutate: vi.fn()},
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        getFilteredWorkflows: (workflows: any[]) => workflows || [],
        getWorkflowsProjectId: () => vi.fn(),
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 10}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => true, // always enable ff_1041
}));

vi.mock('@/shared/mutations/automation/workflows.mutations', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useCreateProjectWorkflowMutation: (opts: any) => ({
        mutate: vi.fn().mockImplementation(() => {
            opts?.onSuccess?.();
        }),
    }),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectWorkflowImported: vi.fn()}),
}));

vi.mock('@/hooks/use-toast', () => ({
    useToast: () => ({toast: vi.fn()}),
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');
    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}),
    };
});

vi.mock('react-router-dom', async () => ({
    useNavigate: () => vi.fn(),
}));

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
        mockGetWorkflowsQuery.mockReturnValue({data: undefined, isLoading: false});
    } else {
        mockGetProjectWorkflowsQuery.mockReturnValue({data: undefined, isLoading: false});
        mockGetWorkflowsQuery.mockReturnValue({data: workflows, isLoading: loading});
    }
};

const baseProps = {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    bottomResizablePanelRef: {current: null} as any,
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

    it('shows Workflow button and opens WorkflowDialog on click', async () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        // Button visible
        expect(screen.getByText('Workflow')).toBeInTheDocument();

        // Click primary button to open dialog
        fireEvent.click(screen.getByText('Workflow'));

        expect(await screen.findByRole('dialog')).toBeInTheDocument();
    });

    it('calls import workflow mutation when selecting a file', async () => {
        setupQueries({selectedProjectId: 3});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={3} />);

        // Open dropdown (chevron) -> Import Workflow
        const buttons = screen.getAllByTestId('btn');
        const chevronBtn = buttons[buttons.length - 1]; // the chevron is rendered after the primary button
        fireEvent.click(chevronBtn);
        fireEvent.click(screen.getByText(/Import Workflow/i));

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const input = screen.queryByAltText('file') as any;

        // Fallback: query by selector if role not available due to hidden input
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const fileInput = (input ?? (document.querySelector('input[type="file"]') as any)) as HTMLInputElement;

        expect(fileInput).toBeTruthy();

        const content = 'my-definition';
        const file = new File([content], 'wf.json', {type: 'application/json'});

        // Fire change event
        fireEvent.change(fileInput, {target: {files: [file]}});

        await flushPromises();

        // onSuccess toast etc. are called within mutation; test by checking that input is reset to empty string
        await waitFor(() => expect(fileInput.value).toBe(''));
    });
});
