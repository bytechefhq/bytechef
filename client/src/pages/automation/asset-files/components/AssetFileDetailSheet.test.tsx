import {assetFilesStore} from '@/pages/automation/asset-files/stores/useAssetFilesStore';
import {AssetFileSource} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AssetFileDetailSheet from './AssetFileDetailSheet';

const hoisted = vi.hoisted(() => ({
    mockUseAssetFileContent: vi.fn(),
    mockUseGetAssetFileQuery: vi.fn(),
    mockUseGetAssetFileTextContentQuery: vi.fn(),
    mockUseGetAssetFileVersionsQuery: vi.fn(),
    mockUseRestoreAssetFileVersionMutation: vi.fn(),
    mockUseUpdateAssetFileTextContentMutation: vi.fn(),
}));

vi.mock('@/shared/components/asset-file-viewer/useAssetFileContent', () => ({
    default: hoisted.mockUseAssetFileContent,
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useGetAssetFileQuery: hoisted.mockUseGetAssetFileQuery,
        useGetAssetFileTextContentQuery: hoisted.mockUseGetAssetFileTextContentQuery,
        useGetAssetFileVersionsQuery: hoisted.mockUseGetAssetFileVersionsQuery,
        useRestoreAssetFileVersionMutation: hoisted.mockUseRestoreAssetFileVersionMutation,
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

    hoisted.mockUseAssetFileContent.mockReturnValue({content: '# Hello', loading: false, mimeType: 'text/markdown'});
    hoisted.mockUseGetAssetFileTextContentQuery.mockReturnValue({
        data: {assetFileTextContent: '# Hello'},
    });
    hoisted.mockUseGetAssetFileVersionsQuery.mockReturnValue({data: {assetFileVersions: []}});
    hoisted.mockUseRestoreAssetFileVersionMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
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
    it('renders a rendered markdown preview for text/markdown files by default', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({data: {assetFile: {...baseFile}}});

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('asset-file-markdown-preview')).toBeInTheDocument();
    });

    it('switches from preview to Monaco editor via the Edit toggle', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({data: {assetFile: {...baseFile}}});

        render(<AssetFileDetailSheet />);

        const editToggle = await screen.findByTestId('asset-file-edit-toggle');

        await userEvent.click(editToggle);

        expect(await screen.findByTestId('monaco-editor-mock')).toBeInTheDocument();
    });

    it('renders Monaco editor directly for non-previewable text files', async () => {
        hoisted.mockUseAssetFileContent.mockReturnValue({
            content: 'plain notes',
            loading: false,
            mimeType: 'text/plain',
        });
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({
            data: {
                assetFile: {
                    ...baseFile,
                    id: '4',
                    mimeType: 'text/plain',
                    name: 'notes.txt',
                },
            },
        });

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('monaco-editor-mock')).toBeInTheDocument();
    });

    it('renders a sandboxed iframe preview for text/html files', async () => {
        hoisted.mockUseAssetFileContent.mockReturnValue({
            content: '<h1>hi</h1>',
            loading: false,
            mimeType: 'text/html',
        });
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({
            data: {
                assetFile: {
                    ...baseFile,
                    id: '5',
                    mimeType: 'text/html',
                    name: 'dashboard.html',
                },
            },
        });
        hoisted.mockUseGetAssetFileTextContentQuery.mockReturnValue({
            data: {assetFileTextContent: '<h1>hi</h1>'},
        });

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('asset-file-html-preview')).toBeInTheDocument();
    });

    it('renders a CSV table preview for text/csv files', async () => {
        hoisted.mockUseAssetFileContent.mockReturnValue({content: 'a,b\n1,2', loading: false, mimeType: 'text/csv'});
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({
            data: {
                assetFile: {
                    ...baseFile,
                    id: '6',
                    mimeType: 'text/csv',
                    name: 'data.csv',
                },
            },
        });
        hoisted.mockUseGetAssetFileTextContentQuery.mockReturnValue({
            data: {assetFileTextContent: 'a,b\n1,2'},
        });

        render(<AssetFileDetailSheet />);

        expect(await screen.findByTestId('asset-file-csv-preview')).toBeInTheDocument();
        expect(await screen.findByText('b')).toBeInTheDocument();
    });

    it('shows the empty version history state via the history toggle', async () => {
        hoisted.mockUseGetAssetFileQuery.mockReturnValue({data: {assetFile: {...baseFile}}});

        render(<AssetFileDetailSheet />);

        const historyToggle = await screen.findByTestId('asset-file-history-toggle');

        await userEvent.click(historyToggle);

        expect(await screen.findByTestId('asset-file-versions')).toBeInTheDocument();
        expect(await screen.findByText(/No previous versions/)).toBeInTheDocument();
    });

    it('renders an img element for image/png files', async () => {
        hoisted.mockUseAssetFileContent.mockReturnValue({content: '', loading: false, mimeType: 'image/png'});
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
        // The viewer builds content URLs from the sheet's selected file id, so keep the store selection and the
        // mocked row id consistent the way they always are in production.
        assetFilesStore.setState({selectedFileId: 7});

        hoisted.mockUseAssetFileContent.mockReturnValue({content: '', loading: false, mimeType: 'application/pdf'});
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
        hoisted.mockUseAssetFileContent.mockReturnValue({
            content: '',
            loading: false,
            mimeType: 'application/octet-stream',
        });
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
