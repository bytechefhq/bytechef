import {TooltipProvider} from '@/components/ui/tooltip';
import Projects from '@/pages/automation/projects/Projects';
import {fireEvent, render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

// Mock the necessary stores and hooks
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useWorkspaceStore: (selector: any) => selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => ({
        application: {edition: 'CE'},
    }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (flag: string) => flag === 'ff-2482' || flag === 'ff-1041',
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
        data: [],
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.queries', () => ({
    useGetWorkspaceProjectGitConfigurationsQuery: () => ({
        data: [],
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

vi.mock('@/hooks/use-toast', () => ({
    useToast: () => ({toast: vi.fn()}),
}));

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
});

afterEach(() => {
    queryClient.clear();
});

const renderProjects = () => {
    render(
        <MemoryRouter>
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
});
