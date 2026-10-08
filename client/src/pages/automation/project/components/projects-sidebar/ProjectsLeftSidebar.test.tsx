import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
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

// Mocks for UI components used inside the scroll area
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

// Hooks and stores
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

    it('leaves project and workflow creation to the settings menu', async () => {
        setupQueries({selectedProjectId: 9});

        renderWithProviders(<ProjectsLeftSidebar {...baseProps} projectId={9} />);

        await screen.findAllByTestId('workflow-item');

        expect(screen.queryByLabelText('New project')).not.toBeInTheDocument();
        expect(screen.queryByText('Workflow')).not.toBeInTheDocument();
        expect(screen.queryByText('Import Workflow')).not.toBeInTheDocument();
        expect(document.querySelector('input[type="file"]')).toBeNull();
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
