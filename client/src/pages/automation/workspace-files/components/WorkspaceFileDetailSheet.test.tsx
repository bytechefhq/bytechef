import {workspaceFilesStore} from '@/pages/automation/workspace-files/stores/useWorkspaceFilesStore';
import {WorkspaceFileSource} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceFileDetailSheet from './WorkspaceFileDetailSheet';

const hoisted = vi.hoisted(() => ({
    mockUseGetWorkspaceFileQuery: vi.fn(),
    mockUseGetWorkspaceFileTextContentQuery: vi.fn(),
    mockUseUpdateWorkspaceFileTagsMutation: vi.fn(),
    mockUseUpdateWorkspaceFileTextContentMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useGetWorkspaceFileQuery: hoisted.mockUseGetWorkspaceFileQuery,
        useGetWorkspaceFileTextContentQuery: hoisted.mockUseGetWorkspaceFileTextContentQuery,
        useUpdateWorkspaceFileTagsMutation: hoisted.mockUseUpdateWorkspaceFileTagsMutation,
        useUpdateWorkspaceFileTextContentMutation: hoisted.mockUseUpdateWorkspaceFileTextContentMutation,
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
    source: WorkspaceFileSource.UserUpload,
    tags: [] as Array<{id: string; name: string}>,
};

beforeEach(() => {
    windowResizeObserver();

    workspaceFilesStore.setState({
        searchQuery: '',
        selectedFileId: 1,
        selectedTagIds: [],
    });

    hoisted.mockUseGetWorkspaceFileTextContentQuery.mockReturnValue({
        data: {workspaceFileTextContent: '# Hello'},
    });
    hoisted.mockUseUpdateWorkspaceFileTextContentMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
    hoisted.mockUseUpdateWorkspaceFileTagsMutation.mockReturnValue({isPending: false, mutate: vi.fn()});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();

    workspaceFilesStore.setState({
        searchQuery: '',
        selectedFileId: null,
        selectedTagIds: [],
    });
});

describe('WorkspaceFileDetailSheet', () => {
    it('renders Monaco editor for text/markdown files', async () => {
        hoisted.mockUseGetWorkspaceFileQuery.mockReturnValue({data: {workspaceFile: {...baseFile}}});

        render(<WorkspaceFileDetailSheet />);

        expect(await screen.findByTestId('monaco-editor-mock')).toBeInTheDocument();
    });

    it('renders an img element for image/png files', async () => {
        hoisted.mockUseGetWorkspaceFileQuery.mockReturnValue({
            data: {
                workspaceFile: {
                    ...baseFile,
                    id: '2',
                    mimeType: 'image/png',
                    name: 'screenshot.png',
                },
            },
        });

        render(<WorkspaceFileDetailSheet />);

        expect(await screen.findByTestId('workspace-file-image')).toBeInTheDocument();
    });

    it('renders a Download button for application/octet-stream files', async () => {
        hoisted.mockUseGetWorkspaceFileQuery.mockReturnValue({
            data: {
                workspaceFile: {
                    ...baseFile,
                    id: '3',
                    mimeType: 'application/octet-stream',
                    name: 'archive.bin',
                },
            },
        });

        render(<WorkspaceFileDetailSheet />);

        expect(await screen.findByTestId('workspace-file-download')).toBeInTheDocument();
    });
});
