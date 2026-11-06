import {TooltipProvider} from '@/components/ui/tooltip';
import {AssetFileSource} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AssetFiles from './AssetFiles';

const hoisted = vi.hoisted(() => ({
    mockUseDeleteAssetFileMutation: vi.fn(),
    mockUseGetAssetFileQuery: vi.fn(),
    mockUseGetAssetFileTagsQuery: vi.fn(),
    mockUseGetAssetFileTextContentQuery: vi.fn(),
    mockUseGetAssetFilesQuery: vi.fn(),
    mockUseUpdateAssetFileMutation: vi.fn(),
    mockUseUpdateAssetFileTextContentMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useDeleteAssetFileMutation: hoisted.mockUseDeleteAssetFileMutation,
        useGetAssetFileQuery: hoisted.mockUseGetAssetFileQuery,
        useGetAssetFileTagsQuery: hoisted.mockUseGetAssetFileTagsQuery,
        useGetAssetFileTextContentQuery: hoisted.mockUseGetAssetFileTextContentQuery,
        useGetAssetFilesQuery: hoisted.mockUseGetAssetFilesQuery,
        useUpdateAssetFileMutation: hoisted.mockUseUpdateAssetFileMutation,
        useUpdateAssetFileTextContentMutation: hoisted.mockUseUpdateAssetFileTextContentMutation,
    };
});

const renderPage = () =>
    render(
        <TooltipProvider>
            <MemoryRouter>
                <AssetFiles />
            </MemoryRouter>
        </TooltipProvider>
    );

beforeEach(() => {
    windowResizeObserver();

    hoisted.mockUseGetAssetFileTagsQuery.mockReturnValue({data: {assetFileTags: []}});
    hoisted.mockUseGetAssetFileQuery.mockReturnValue({data: null});
    hoisted.mockUseGetAssetFileTextContentQuery.mockReturnValue({data: null});

    hoisted.mockUseDeleteAssetFileMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
    hoisted.mockUseUpdateAssetFileMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
    hoisted.mockUseUpdateAssetFileTextContentMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AssetFiles', () => {
    it('renders empty state when no files returned', () => {
        hoisted.mockUseGetAssetFilesQuery.mockReturnValue({
            data: {assetFiles: []},
            error: null,
            isLoading: false,
            refetch: vi.fn(),
        });

        renderPage();

        expect(screen.getByText('No Files')).toBeInTheDocument();
    });

    it('renders a row when a file is returned', () => {
        hoisted.mockUseGetAssetFilesQuery.mockReturnValue({
            data: {
                assetFiles: [
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
                        source: AssetFileSource.UserUpload,
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
        expect(screen.getByTestId('asset-file-list-item-1')).toBeInTheDocument();
    });
});
