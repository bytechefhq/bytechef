import {WorkspaceFileSource} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceFiles from './WorkspaceFiles';

const hoisted = vi.hoisted(() => ({
    mockUseDeleteWorkspaceFileMutation: vi.fn(),
    mockUseGetWorkspaceFileQuery: vi.fn(),
    mockUseGetWorkspaceFileTagsQuery: vi.fn(),
    mockUseGetWorkspaceFileTextContentQuery: vi.fn(),
    mockUseGetWorkspaceFilesQuery: vi.fn(),
    mockUseUpdateWorkspaceFileMutation: vi.fn(),
    mockUseUpdateWorkspaceFileTagsMutation: vi.fn(),
    mockUseUpdateWorkspaceFileTextContentMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useDeleteWorkspaceFileMutation: hoisted.mockUseDeleteWorkspaceFileMutation,
        useGetWorkspaceFileQuery: hoisted.mockUseGetWorkspaceFileQuery,
        useGetWorkspaceFileTagsQuery: hoisted.mockUseGetWorkspaceFileTagsQuery,
        useGetWorkspaceFileTextContentQuery: hoisted.mockUseGetWorkspaceFileTextContentQuery,
        useGetWorkspaceFilesQuery: hoisted.mockUseGetWorkspaceFilesQuery,
        useUpdateWorkspaceFileMutation: hoisted.mockUseUpdateWorkspaceFileMutation,
        useUpdateWorkspaceFileTagsMutation: hoisted.mockUseUpdateWorkspaceFileTagsMutation,
        useUpdateWorkspaceFileTextContentMutation: hoisted.mockUseUpdateWorkspaceFileTextContentMutation,
    };
});

const renderPage = () =>
    render(
        <MemoryRouter>
            <WorkspaceFiles />
        </MemoryRouter>
    );

beforeEach(() => {
    windowResizeObserver();

    hoisted.mockUseGetWorkspaceFileTagsQuery.mockReturnValue({data: {workspaceFileTags: []}});
    hoisted.mockUseGetWorkspaceFileQuery.mockReturnValue({data: null});
    hoisted.mockUseGetWorkspaceFileTextContentQuery.mockReturnValue({data: null});

    hoisted.mockUseDeleteWorkspaceFileMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
    hoisted.mockUseUpdateWorkspaceFileMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
    hoisted.mockUseUpdateWorkspaceFileTagsMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
    hoisted.mockUseUpdateWorkspaceFileTextContentMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('WorkspaceFiles', () => {
    it('renders empty state when no files returned', () => {
        hoisted.mockUseGetWorkspaceFilesQuery.mockReturnValue({
            data: {workspaceFiles: []},
            error: null,
            isLoading: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('No Files')).toBeInTheDocument();
    });

    it('renders a row when a file is returned', () => {
        hoisted.mockUseGetWorkspaceFilesQuery.mockReturnValue({
            data: {
                workspaceFiles: [
                    {
                        createdBy: 'user@localhost',
                        createdDate: 1700000000000,
                        description: null,
                        downloadUrl: '/downloads/1',
                        generatedByAgentSource: null,
                        generatedFromPrompt: null,
                        id: '1',
                        lastModifiedBy: 'user@localhost',
                        lastModifiedDate: 1700000000000,
                        mimeType: 'text/markdown',
                        name: 'spec.md',
                        sizeBytes: 128,
                        source: WorkspaceFileSource.UserUpload,
                        tags: [],
                    },
                ],
            },
            error: null,
            isLoading: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('spec.md')).toBeInTheDocument();
        expect(screen.getByTestId('workspace-file-row-1')).toBeInTheDocument();
    });
});
