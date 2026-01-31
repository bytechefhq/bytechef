import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentListItem from '../KnowledgeBaseDocumentListItem';

const hoisted = vi.hoisted(() => {
    return {
        handleDocumentListItemClick: vi.fn(),
        handleDocumentListItemKeyDown: vi.fn(),
        handleTagListClick: vi.fn(),
        mockUseKnowledgeBaseDocumentListItem: vi.fn(),
        mockUseKnowledgeBaseDocumentListItemTagList: vi.fn(),
        updateTagsMutation: {mutate: vi.fn()},
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentListItem', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentListItem,
}));

vi.mock('../hooks/useKnowledgeBaseDocumentListItemTagList', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentListItemTagList,
}));

vi.mock('../KnowledgeBaseDocumentListItemDropdownMenu', () => ({
    default: ({documentId}: {documentId: string}) => <div data-testid={`dropdown-${documentId}`}>Dropdown</div>,
}));

vi.mock('@/shared/components/TagList', () => ({
    default: () => <div data-testid="tag-list">Tag List</div>,
}));

vi.mock('@/components/ui/collapsible', () => ({
    CollapsibleTrigger: ({children, ref}: {children: React.ReactNode; ref?: React.Ref<HTMLButtonElement>}) => (
        <button data-testid="collapsible-trigger" ref={ref}>
            {children}
        </button>
    ),
}));

const mockDocument: KnowledgeBaseDocument = {
    chunks: [
        {content: 'Content 1', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null},
        {content: 'Content 2', id: 'chunk-2', knowledgeBaseDocumentId: 'doc-1', metadata: null},
    ],
    createdDate: '2024-01-15T10:30:00Z',
    document: {
        extension: 'pdf',
        mimeType: 'application/pdf',
        name: 'test-file.pdf',
        url: 'https://example.com/test-file.pdf',
    },
    id: 'doc-1',
    name: 'Test Document',
    status: 2,
};

const mockTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
];

const mockRemainingTags = [{id: '3', name: 'Tag 3'}];

const defaultListItemMockReturn = {
    chunkCount: 2,
    chunksCollapsibleTriggerRef: {current: null},
    displayName: 'test-file.pdf',
    documentIcon: <span data-testid="document-icon">Icon</span>,
    handleDocumentListItemClick: hoisted.handleDocumentListItemClick,
    handleDocumentListItemKeyDown: hoisted.handleDocumentListItemKeyDown,
    handleTagListClick: hoisted.handleTagListClick,
    statusBadge: <span data-testid="status-badge">Ready</span>,
};

const defaultTagListMockReturn = {
    convertedRemainingTags: mockRemainingTags.map((tag) => ({...tag, id: Number(tag.id)})),
    convertedTags: mockTags.map((tag) => ({...tag, id: Number(tag.id)})),
    updateTagsMutation: hoisted.updateTagsMutation,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentListItem.mockReturnValue({...defaultListItemMockReturn});
    hoisted.mockUseKnowledgeBaseDocumentListItemTagList.mockReturnValue({...defaultTagListMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (
    props: {document: KnowledgeBaseDocument; remainingTags?: typeof mockRemainingTags; tags: typeof mockTags} = {
        document: mockDocument,
        remainingTags: mockRemainingTags,
        tags: mockTags,
    }
) => {
    return render(<KnowledgeBaseDocumentListItem {...props} />);
};

describe('KnowledgeBaseDocumentListItem', () => {
    it('renders the document icon', () => {
        renderComponent();

        expect(screen.getByTestId('document-icon')).toBeInTheDocument();
    });

    it('renders the display name', () => {
        renderComponent();

        expect(screen.getByText('test-file.pdf')).toBeInTheDocument();
    });

    it('renders the chunk count', () => {
        renderComponent();

        expect(screen.getByText('2 chunks')).toBeInTheDocument();
    });

    it('renders singular chunk for count of 1', () => {
        hoisted.mockUseKnowledgeBaseDocumentListItem.mockReturnValue({
            ...defaultListItemMockReturn,
            chunkCount: 1,
        });

        renderComponent();

        expect(screen.getByText('1 chunk')).toBeInTheDocument();
    });

    it('renders the status badge', () => {
        renderComponent();

        expect(screen.getByTestId('status-badge')).toBeInTheDocument();
    });

    it('renders the dropdown menu', () => {
        renderComponent();

        expect(screen.getByTestId('dropdown-doc-1')).toBeInTheDocument();
    });

    it('renders the tag list', () => {
        renderComponent();

        expect(screen.getByTestId('tag-list')).toBeInTheDocument();
    });

    it('renders the collapsible trigger', () => {
        renderComponent();

        expect(screen.getByTestId('collapsible-trigger')).toBeInTheDocument();
    });

    it('calls handleDocumentListItemClick when clicked', async () => {
        renderComponent();

        const container = screen.getByText('test-file.pdf').closest('[role="button"]');
        await userEvent.click(container!);

        expect(hoisted.handleDocumentListItemClick).toHaveBeenCalled();
    });

    it('has correct accessibility attributes', () => {
        renderComponent();

        const container = screen.getByText('test-file.pdf').closest('[role="button"]');

        expect(container).toHaveAttribute('tabIndex', '0');
    });

    it('renders created date', () => {
        renderComponent();

        expect(screen.getByText(/Created/)).toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentListItem without created date', () => {
    it('does not render created date when not available', () => {
        const documentWithoutDate = {...mockDocument, createdDate: undefined};

        renderComponent({document: documentWithoutDate, remainingTags: mockRemainingTags, tags: mockTags});

        expect(screen.queryByText(/Created/)).not.toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentListItem hook calls', () => {
    it('passes document to useKnowledgeBaseDocumentListItem', () => {
        renderComponent();

        expect(hoisted.mockUseKnowledgeBaseDocumentListItem).toHaveBeenCalledWith({
            document: mockDocument,
        });
    });

    it('passes correct props to useKnowledgeBaseDocumentListItemTagList', () => {
        renderComponent();

        expect(hoisted.mockUseKnowledgeBaseDocumentListItemTagList).toHaveBeenCalledWith({
            knowledgeBaseDocumentId: 'doc-1',
            remainingTags: mockRemainingTags,
            tags: mockTags,
        });
    });
});
