import {assetFilesStore} from '@/pages/automation/asset-files/stores/useAssetFilesStore';
import {AssetFileSource} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AssetFileDetailSheet from './AssetFileDetailSheet';

const hoisted = vi.hoisted(() => ({
    mockUseGetAssetFileQuery: vi.fn(),
    mockUseGetAssetFileTextContentQuery: vi.fn(),
    mockUseUpdateAssetFileTextContentMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useGetAssetFileQuery: hoisted.mockUseGetAssetFileQuery,
        useGetAssetFileTextContentQuery: hoisted.mockUseGetAssetFileTextContentQuery,
        useUpdateAssetFileTextContentMutation: hoisted.mockUseUpdateAssetFileTextContentMutation,
    };
});

vi.mock('@/shared/components/MonacoEditorWrapper', () => ({
    default: ({defaultLanguage, value}: {defaultLanguage: string; value: string}) => (
        <div data-language={defaultLanguage} data-testid="monaco-editor-mock">
            {value}
        </div>
    ),
}));

const baseFile = {
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
    tags: [] as Array<{id: string; name: string}>,
};

beforeEach(() => {
    windowResizeObserver();

    assetFilesStore.setState({
        searchQuery: '',
        selectedFileId: 1,
        selectedTagIds: [],
    });

    hoisted.mockUseGetAssetFileTextContentQuery.mockReturnValue({
        data: {assetFileTextContent: '# Hello'},
    });
    hoisted.mockUseUpdateAssetFileTextContentMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();

    assetFilesStore.setState({
        searchQuery: '',
        selectedFileId: null,
        selectedTagIds: [],
    });
});

describe('AssetFileDetailSheet', () => {
    it('renders Monaco editor for text/markdown files', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({data: {assetFile: {...baseFile}}});

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('monaco-editor-mock')).toBeInTheDocument();
    });

    it('renders an img element for image/png files', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({
            data: {
                assetFile: {
                    ...baseFile,
                    id: '2',
                    mimeType: 'image/png',
                    name: 'screenshot.png',
                },
            },
        });

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('asset-file-image')).toBeInTheDocument();
    });

    it('renders an iframe with inline disposition for application/pdf files', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({
            data: {
                assetFile: {
                    ...baseFile,
                    downloadUrl: '/api/automation/internal/asset-files/7/content',
                    id: '7',
                    mimeType: 'application/pdf',
                    name: 'report.pdf',
                },
            },
        });

        render(<AssetFileDetailSheet />);

        const iframe = await screen.findByTestId('asset-file-iframe');

        expect(iframe).toHaveAttribute('src', '/api/automation/internal/asset-files/7/content?disposition=inline');
    });

    it('renders a Download button for application/octet-stream files', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({
            data: {
                assetFile: {
                    ...baseFile,
                    id: '3',
                    mimeType: 'application/octet-stream',
                    name: 'archive.bin',
                },
            },
        });

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('asset-file-download')).toBeInTheDocument();
    });
});
