import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentChunkList from '../KnowledgeBaseDocumentChunkList';

const hoisted = vi.hoisted(() => {
    return {
        handleDeleteConfirm: vi.fn(),
        handleDeleteDialogClose: vi.fn(),
        mockUseKnowledgeBaseDocumentChunkDeleteDialog: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentChunkDeleteDialog', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentChunkDeleteDialog,
}));

vi.mock('../KnowledgeBaseDocumentChunkEditDialog', () => ({
    default: () => <div data-testid="edit-dialog">Edit Dialog</div>,
}));

vi.mock('../KnowledgeBaseDocumentChunkListItemDropdownMenu', () => ({
    default: ({chunk}: {chunk: {id: string}}) => <div data-testid={`dropdown-${chunk.id}`}>Dropdown Menu</div>,
}));

vi.mock('../KnowledgeBaseDocumentChunkListItemHeader', () => ({
    default: ({chunkId, chunkIndex}: {chunkId: string; chunkIndex: number}) => (
        <div data-testid={`header-${chunkId}`}>Chunk {chunkIndex + 1}</div>
    ),
}));

vi.mock('../KnowledgeBaseDocumentChunkListSelectionBar', () => ({
    default: () => <div data-testid="selection-bar">Selection Bar</div>,
}));

vi.mock('@/components/DeleteAlertDialog', () => ({
    default: ({onCancel, onDelete, open}: {open: boolean; onCancel: () => void; onDelete: () => void}) =>
        open ? (
            <div data-testid="delete-dialog">
                <button onClick={onCancel}>Cancel</button>

                <button onClick={onDelete}>Delete</button>
            </div>
        ) : null,
}));

const defaultMockReturn = {
    handleClose: hoisted.handleDeleteDialogClose,
    handleConfirm: hoisted.handleDeleteConfirm,
    open: false,
};

const mockChunks = [
    {content: 'First chunk content', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null},
    {content: 'Second chunk content', id: 'chunk-2', knowledgeBaseDocumentId: 'doc-1', metadata: {page: 1}},
];

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentChunkDeleteDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (chunks = mockChunks) => {
    return render(
        <KnowledgeBaseDocumentChunkList chunks={chunks} documentName="Test Document" knowledgeBaseId="kb-1" />
    );
};

describe('KnowledgeBaseDocumentChunkList', () => {
    it('renders empty state when no chunks', () => {
        renderComponent([]);

        expect(screen.getByText('No chunks available for this document.')).toBeInTheDocument();
    });

    it('renders selection bar when chunks exist', () => {
        renderComponent();

        expect(screen.getByTestId('selection-bar')).toBeInTheDocument();
    });

    it('renders all chunks', () => {
        renderComponent();

        expect(screen.getByTestId('header-chunk-1')).toBeInTheDocument();
        expect(screen.getByTestId('header-chunk-2')).toBeInTheDocument();
    });

    it('renders chunk content', () => {
        renderComponent();

        expect(screen.getByText('First chunk content')).toBeInTheDocument();
        expect(screen.getByText('Second chunk content')).toBeInTheDocument();
    });

    it('renders metadata when available', () => {
        renderComponent();

        expect(screen.getByText('Metadata:')).toBeInTheDocument();
        expect(screen.getByText('{"page":1}')).toBeInTheDocument();
    });

    it('renders dropdown menu for each chunk', () => {
        renderComponent();

        expect(screen.getByTestId('dropdown-chunk-1')).toBeInTheDocument();
        expect(screen.getByTestId('dropdown-chunk-2')).toBeInTheDocument();
    });

    it('renders edit dialog', () => {
        renderComponent();

        expect(screen.getByTestId('edit-dialog')).toBeInTheDocument();
    });

    it('does not render delete dialog when closed', () => {
        renderComponent();

        expect(screen.queryByTestId('delete-dialog')).not.toBeInTheDocument();
    });

    it('renders delete dialog when open', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkDeleteDialog.mockReturnValue({
            ...defaultMockReturn,
            open: true,
        });

        renderComponent();

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentChunkList with single chunk', () => {
    it('renders single chunk correctly', () => {
        renderComponent([{content: 'Only chunk', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null}]);

        expect(screen.getByText('Only chunk')).toBeInTheDocument();
        expect(screen.getByTestId('header-chunk-1')).toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentChunkList chunk without metadata', () => {
    it('does not render metadata section when metadata is null', () => {
        renderComponent([
            {content: 'Chunk without metadata', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null},
        ]);

        expect(screen.queryByText('Metadata:')).not.toBeInTheDocument();
    });
});
