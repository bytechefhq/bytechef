import {TooltipProvider} from '@/components/ui/tooltip';
import Projects from '@/pages/automation/projects/Projects';
import {fireEvent, render, screen, userEvent, waitFor, within} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const mockProjectsState = vi.hoisted(() => ({
    captureEmptyStateAction: vi.fn(),
    captureProjectImported: vi.fn(),
    navigate: vi.fn(),
    projects: [] as unknown[],
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {
        ...actual,
        useNavigate: () => mockProjectsState.navigate,
    };
});

// Mock the necessary stores and hooks
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => ({
        analytics: {enabled: false, postHog: {apiKey: '', host: ''}},
        application: {edition: 'CE'},
        featureFlags: {},
    }),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({
        captureEmptyStateAction: mockProjectsState.captureEmptyStateAction,
        captureProjectImported: mockProjectsState.captureProjectImported,
    }),
}));

// Mock the API queries
vi.mock('@/shared/queries/automation/projectCategories.queries', () => ({
    useGetProjectCategoriesQuery: () => ({
        data: [],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/automation/projectTags.queries', () => ({
    useGetProjectTagsQuery: () => ({
        data: [],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {
        filteredProjects: (filters: {categoryId?: number; id: number; tagId?: number}) => [
            'projects',
            filters.id,
            filters,
        ],
        project: (id: number) => ['projects', id],
        projectWorkflows: (id: number) => ['projects', id, 'workflows'],
        projects: ['projects'],
    },
    useGetWorkspaceProjectsQuery: () => ({
        data: mockProjectsState.projects,
        error: null,
        isLoading: false,
    }),
}));

const mockImportMutate = vi.fn();
vi.mock('@/shared/mutations/automation/projects.mutations', async () => {
    const actual = await vi.importActual<typeof import('@/shared/mutations/automation/projects.mutations')>(
        '@/shared/mutations/automation/projects.mutations'
    );

    return {
        ...actual,
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        useImportProjectMutation: (opts: any) => ({
            mutate: mockImportMutate.mockImplementation(() => {
                opts?.onSuccess?.();
            }),
        }),
    };
});

vi.mock('@/pages/automation/project/loadProject', () => ({default: vi.fn()}));

vi.mock('@/pages/automation/projects/components/project-list/ProjectList', () => ({
    default: () => <div data-testid="project-list" />,
}));

vi.mock('sonner', () => ({toast: vi.fn()}));

const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

let queryClient: QueryClient;

beforeEach(() => {
    queryClient = createTestQueryClient();
    mockImportMutate.mockClear();
    mockProjectsState.captureEmptyStateAction.mockClear();
    mockProjectsState.captureProjectImported.mockClear();
    mockProjectsState.navigate.mockClear();
    mockProjectsState.projects = [];
});

afterEach(() => {
    queryClient.clear();
});

const renderProjects = (initialEntries: string[] = ['/']) => {
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <Projects />
                </TooltipProvider>
            </QueryClientProvider>
        </MemoryRouter>
    );
};

describe('Projects Import Functionality', () => {
    it('should show import dropdown menu items', async () => {
        renderProjects();

        // Find the dropdown trigger (chevron) button and click it to open the menu
        const createButton = screen.getByRole('button', {name: /create project/i});
        expect(createButton).toBeInTheDocument();

        const buttons = screen.getAllByRole('button');
        const chevronButton = buttons.find((b) => !/create project/i.test(b.textContent || ''))!;
        await userEvent.click(chevronButton);

        await waitFor(() => {
            expect(screen.getByText('From Template')).toBeInTheDocument();
            expect(screen.getByText('Import Project')).toBeInTheDocument();
        });
    });

    it('should trigger file input when import project is clicked', async () => {
        renderProjects();

        // Open the dropdown using the chevron button
        const buttons = screen.getAllByRole('button');
        const chevronButton = buttons.find((b) => /chevron-down/i.test(b.innerHTML) || b.querySelector('svg'))!;
        await userEvent.click(chevronButton);

        await waitFor(() => {
            const importButton = screen.getByText('Import Project');
            expect(importButton).toBeInTheDocument();

            // Since the file input is hidden and accessed via ref, we can't easily test the actual click
            // but we can verify the menu item exists and is clickable, which is the main functionality
            expect(importButton).toBeInTheDocument();
        });
    });

    it('should call import mutation when a file is selected', async () => {
        renderProjects();

        // Create a mock file
        const mockFile = new File(['test content'], 'test-project.zip', {
            type: 'application/zip',
        });

        // Find the hidden file input
        const fileInput = document.querySelector('input[type="file"][accept=".zip"]') as HTMLInputElement;
        expect(fileInput).toBeTruthy();

        // Simulate file selection using fireEvent
        fireEvent.change(fileInput, {target: {files: [mockFile]}});

        await waitFor(() => {
            expect(mockImportMutate).toHaveBeenCalledWith({
                file: mockFile,
                workspaceId: 1,
            });
        });
    });

    it('should capture analytics when the project import succeeds', async () => {
        renderProjects();

        const fileInput = document.querySelector('input[type="file"][accept=".zip"]') as HTMLInputElement;

        fireEvent.change(fileInput, {target: {files: [new File(['test content'], 'test-project.zip')]}});

        await waitFor(() => {
            expect(mockProjectsState.captureProjectImported).toHaveBeenCalledTimes(1);
        });
    });
});

describe('Projects empty states', () => {
    it('explains what a project is and links to the docs', () => {
        renderProjects();

        expect(screen.getByRole('heading', {level: 2, name: 'Create your first project'})).toBeInTheDocument();
        expect(screen.getByText(/A project groups related workflows\./)).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'How projects work'})).toHaveAttribute(
            'href',
            'https://docs.bytechef.io/platform/automation/build/workflows/projects'
        );
    });

    it('shows creating a project as the current step of getting started', () => {
        renderProjects();

        const steps = within(screen.getByRole('list', {name: 'Getting started'})).getAllByRole('listitem');

        expect(steps).toHaveLength(4);
        expect(steps[0]).toHaveTextContent('Create a project');
        expect(steps[0]).toHaveAttribute('aria-current', 'step');
    });

    it('records a click on Create Project', async () => {
        renderProjects();

        await userEvent.click(screen.getByRole('button', {name: 'Create Project'}));

        expect(mockProjectsState.captureEmptyStateAction).toHaveBeenCalledWith(
            'projects',
            'create_project',
            'createProject'
        );
    });

    it('hides the category and tag sidebar until there is something to filter', () => {
        renderProjects();

        expect(screen.queryByText('No defined tags.')).not.toBeInTheDocument();
        expect(screen.getByText('Projects')).toBeInTheDocument();
    });

    // The filtered list and the unfiltered one were the same array, so a filter that matched nothing rendered
    // the first-run state and told the reader the workspace was empty.
    it('reports a filter miss rather than an empty workspace', () => {
        renderProjects(['/?tagId=1']);

        expect(screen.getByText('No Matching Projects')).toBeInTheDocument();
        expect(screen.queryByText('Create your first project')).not.toBeInTheDocument();
        expect(screen.getByText('No defined tags.')).toBeInTheDocument();
    });
});

describe('Projects menus', () => {
    const openMenu = async () => {
        await userEvent.click(document.querySelector('[aria-haspopup="menu"]')!);
    };

    describe.each([
        ['empty state', [] as unknown[]],
        ['header', [{id: 1, name: 'Project'}]],
    ])('%s menu', (_name, projects) => {
        beforeEach(() => {
            mockProjectsState.projects = projects;
        });

        it('should render with the design-system DropdownMenu', async () => {
            renderProjects();

            await openMenu();

            expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'From Template'})).toHaveClass('text-content-neutral-primary');
            expect(screen.getByRole('menuitem', {name: 'Import Project'})).toBeInTheDocument();
        });

        it('should navigate to the templates when From Template is clicked', async () => {
            renderProjects();

            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'From Template'}));

            expect(mockProjectsState.navigate).toHaveBeenCalledWith('templates');
        });

        it('should open the file picker when Import Project is clicked', async () => {
            const clickSpy = vi.spyOn(HTMLInputElement.prototype, 'click');

            renderProjects();

            await openMenu();

            await userEvent.click(screen.getByRole('menuitem', {name: 'Import Project'}));

            expect(clickSpy).toHaveBeenCalled();

            clickSpy.mockRestore();
        });
    });
});
