import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentList from '../KnowledgeBaseDocumentList';

const hoisted = vi.hoisted(() => {
    return {
        handleDeleteConfirm: vi.fn(),
        handleDeleteDialogClose: vi.fn(),
        mockUseKnowledgeBaseDocumentList: vi.fn(),
        mockUseKnowledgeBaseDocumentListItemDeleteDialog: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentList', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentList,
}));

vi.mock('../hooks/useKnowledgeBaseDocumentListItemDeleteDialog', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentListItemDeleteDialog,
}));

vi.mock('../KnowledgeBaseDocumentListItem', () => ({
    default: ({document}: {document: {id: string; name: string}}) => (
        <div data-testid={`document-item-${document.id}`}>{document.name}</div>
    ),
}));

vi.mock('../KnowledgeBaseDocumentChunkList', () => ({
    default: ({documentName}: {documentName: string}) => (
        <div data-testid={`chunk-list-${documentName}`}>Chunks for {documentName}</div>
    ),
}));

vi.mock('@/components/DeleteAlertDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="delete-dialog">Delete Dialog</div> : null),
}));

vi.mock('@/components/ui/collapsible', () => ({
    Collapsible: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    CollapsibleContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

const mockDocuments = [
    {
        chunks: [{content: 'Content', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null}],
        createdDate: '2024-01-01',
        document: null,
        id: 'doc-1',
        name: 'Document 1',
        status: 2,
    },
    {
        chunks: [],
        createdDate: '2024-01-02',
        document: null,
        id: 'doc-2',
        name: 'Document 2',
        status: 2,
    },
];

const defaultListMockReturn = {
    getRemainingTagsForDocument: vi.fn(() => []),
    getSortedChunksForDocument: vi.fn(() => []),
    getTagsForDocument: vi.fn(() => []),
};

const defaultDeleteDialogMockReturn = {
    handleClose: hoisted.handleDeleteDialogClose,
    handleConfirm: hoisted.handleDeleteConfirm,
    open: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentList.mockReturnValue({...defaultListMockReturn});
    hoisted.mockUseKnowledgeBaseDocumentListItemDeleteDialog.mockReturnValue({...defaultDeleteDialogMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (documents = mockDocuments) => {
    return render(<KnowledgeBaseDocumentList documents={documents} knowledgeBaseId="kb-1" />);
};

describe('KnowledgeBaseDocumentList', () => {
    it('renders empty state when no documents', () => {
        renderComponent([]);

        expect(screen.getByText('No documents available. Upload documents to get started.')).toBeInTheDocument();
    });

    it('renders all documents', () => {
        renderComponent();

        expect(screen.getByTestId('document-item-doc-1')).toBeInTheDocument();
        expect(screen.getByTestId('document-item-doc-2')).toBeInTheDocument();
    });

    it('renders document names', () => {
        renderComponent();

        expect(screen.getByText('Document 1')).toBeInTheDocument();
        expect(screen.getByText('Document 2')).toBeInTheDocument();
    });

    it('renders chunk lists for each document', () => {
        renderComponent();

        expect(screen.getByTestId('chunk-list-Document 1')).toBeInTheDocument();
        expect(screen.getByTestId('chunk-list-Document 2')).toBeInTheDocument();
    });

    it('does not render delete dialog when closed', () => {
        renderComponent();

        expect(screen.queryByTestId('delete-dialog')).not.toBeInTheDocument();
    });

    it('renders delete dialog when open', () => {
        hoisted.mockUseKnowledgeBaseDocumentListItemDeleteDialog.mockReturnValue({
            ...defaultDeleteDialogMockReturn,
            open: true,
        });

        renderComponent();

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });

    it('calls getTagsForDocument for each document', () => {
        const getTagsForDocument = vi.fn(() => []);

        hoisted.mockUseKnowledgeBaseDocumentList.mockReturnValue({
            ...defaultListMockReturn,
            getTagsForDocument,
        });

        renderComponent();

        expect(getTagsForDocument).toHaveBeenCalledWith('doc-1');
        expect(getTagsForDocument).toHaveBeenCalledWith('doc-2');
    });

    it('calls getRemainingTagsForDocument for each document', () => {
        const getRemainingTagsForDocument = vi.fn(() => []);

        hoisted.mockUseKnowledgeBaseDocumentList.mockReturnValue({
            ...defaultListMockReturn,
            getRemainingTagsForDocument,
        });

        renderComponent();

        expect(getRemainingTagsForDocument).toHaveBeenCalledWith('doc-1');
        expect(getRemainingTagsForDocument).toHaveBeenCalledWith('doc-2');
    });

    it('calls getSortedChunksForDocument for each document', () => {
        const getSortedChunksForDocument = vi.fn(() => []);

        hoisted.mockUseKnowledgeBaseDocumentList.mockReturnValue({
            ...defaultListMockReturn,
            getSortedChunksForDocument,
        });

        renderComponent();

        expect(getSortedChunksForDocument).toHaveBeenCalledWith('doc-1');
        expect(getSortedChunksForDocument).toHaveBeenCalledWith('doc-2');
    });
});

describe('KnowledgeBaseDocumentList with single document', () => {
    it('renders single document correctly', () => {
        renderComponent([mockDocuments[0]]);

        expect(screen.getByTestId('document-item-doc-1')).toBeInTheDocument();
        expect(screen.queryByTestId('document-item-doc-2')).not.toBeInTheDocument();
    });
});
