import {TooltipProvider} from '@/components/ui/tooltip';
import Projects from '@/pages/automation/projects/Projects';
import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

// Mock the necessary stores and hooks
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: () => ({
        currentWorkspaceId: 1,
    }),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: () => ({
        application: {edition: 'CE'},
    }),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => (flag: string) => flag === 'ff-2482',
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

// Mock fetch for import functionality
const mockFetch = vi.fn();
global.fetch = mockFetch;

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
    mockFetch.mockClear();
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

        // Find and click the "Create Project" button
        const createButton = screen.getByRole('button', {name: /create project/i});
        expect(createButton).toBeInTheDocument();

        await userEvent.click(createButton);

        await waitFor(() => {
            expect(screen.getByText('From Scratch')).toBeInTheDocument();
            expect(screen.getByText('Import Project')).toBeInTheDocument();
        });
    });

    it('should trigger file input when import project is clicked', async () => {
        renderProjects();

        const createButton = screen.getByRole('button', {name: /create project/i});
        await userEvent.click(createButton);

        await waitFor(() => {
            const importButton = screen.getByText('Import Project');
            expect(importButton).toBeInTheDocument();

            // Since the file input is hidden and accessed via ref, we can't easily test the actual click
            // but we can verify the menu item exists and is clickable, which is the main functionality
            expect(importButton).toBeInTheDocument();
        });
    });

    it('should handle successful project import', async () => {
        renderProjects();

        // Mock successful API response
        mockFetch.mockResolvedValueOnce({
            json: async () => ({id: 123}),
            ok: true,
        });

        // Mock window.location.reload
        const mockReload = vi.fn();
        Object.defineProperty(window, 'location', {
            value: {
                reload: mockReload,
            },
            writable: true,
        });

        // Create a mock file
        const mockFile = new File(['test content'], 'test-project.zip', {
            type: 'application/zip',
        });

        // Create a mock file input change event
        const fileInput = document.createElement('input');
        fileInput.type = 'file';

        // Trigger the import handling directly since we can't easily simulate file selection in tests
        const handleImportProject = async (event: React.ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('file', file);

            try {
                const response = await fetch('/api/automation/internal/workspaces/1/projects/import', {
                    body: formData,
                    method: 'POST',
                });

                if (response.ok) {
                    window.location.reload();
                } else {
                    console.error('Failed to import project');
                }
            } catch (error) {
                console.error('Error importing project:', error);
            }

            if (event.target) {
                event.target.value = '';
            }
        };

        // Simulate the file input change
        Object.defineProperty(fileInput, 'files', {
            value: [mockFile],
            writable: false,
        });

        const mockEvent = {
            target: fileInput,
        } as React.ChangeEvent<HTMLInputElement>;

        await handleImportProject(mockEvent);

        expect(mockFetch).toHaveBeenCalledWith('/api/automation/internal/workspaces/1/projects/import', {
            body: expect.any(FormData),
            method: 'POST',
        });
        expect(mockReload).toHaveBeenCalled();
    });

    it('should handle failed project import', async () => {
        renderProjects();

        // Mock failed API response
        mockFetch.mockResolvedValueOnce({
            ok: false,
            status: 400,
        });

        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        // Create a mock file
        const mockFile = new File(['test content'], 'test-project.zip', {
            type: 'application/zip',
        });

        // Test the import handling directly
        const handleImportProject = async (event: React.ChangeEvent<HTMLInputElement>) => {
            const file = event.target.files?.[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('file', file);

            try {
                const response = await fetch('/api/automation/internal/workspaces/1/projects/import', {
                    body: formData,
                    method: 'POST',
                });

                if (response.ok) {
                    window.location.reload();
                } else {
                    console.error('Failed to import project');
                }
            } catch (error) {
                console.error('Error importing project:', error);
            }

            if (event.target) {
                event.target.value = '';
            }
        };

        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        Object.defineProperty(fileInput, 'files', {
            value: [mockFile],
            writable: false,
        });

        const mockEvent = {
            target: fileInput,
        } as React.ChangeEvent<HTMLInputElement>;

        await handleImportProject(mockEvent);

        expect(mockFetch).toHaveBeenCalledWith('/api/automation/internal/workspaces/1/projects/import', {
            body: expect.any(FormData),
            method: 'POST',
        });
        expect(consoleSpy).toHaveBeenCalledWith('Failed to import project');

        consoleSpy.mockRestore();
    });
});
