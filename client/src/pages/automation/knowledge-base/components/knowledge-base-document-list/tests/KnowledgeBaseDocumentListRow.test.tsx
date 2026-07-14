import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentListRow from '../KnowledgeBaseDocumentListRow';

const hoisted = vi.hoisted(() => {
    return {
        mockUseKnowledgeBaseDocumentChunksQuery: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useKnowledgeBaseDocumentChunksQuery: hoisted.mockUseKnowledgeBaseDocumentChunksQuery,
}));

vi.mock('@/components/ui/collapsible', () => ({
    Collapsible: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    CollapsibleContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('../KnowledgeBaseDocumentListItem', () => ({
    default: ({document}: {document: {id: string; name: string}}) => (
        <div data-testid={`document-item-${document.id}`}>{document.name}</div>
    ),
}));

vi.mock('../KnowledgeBaseDocumentChunkList', () => ({
    default: ({chunks, documentName}: {chunks: Array<{id: string}>; documentName: string}) => (
        <div data-testid={`chunk-list-${documentName}`}>{chunks.length} chunks</div>
    ),
}));

const mockDocument = {
    chunks: [{id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1'}],
    createdDate: '2024-01-01',
    document: null,
    id: 'doc-1',
    name: 'Document 1',
    sourceId: null,
    sourceRecordId: null,
    status: 2,
    tags: null,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentChunksQuery.mockReturnValue({data: undefined, isLoading: false});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderRow = () =>
    render(
        <KnowledgeBaseDocumentListRow
            document={mockDocument as never}
            knowledgeBaseId="kb-1"
            remainingTags={[]}
            tags={[]}
        />
    );

describe('KnowledgeBaseDocumentListRow', () => {
    it('does not fetch chunk content until expanded', () => {
        renderRow();

        expect(hoisted.mockUseKnowledgeBaseDocumentChunksQuery).toHaveBeenCalledWith({id: 'doc-1'}, {enabled: false});
    });

    it('renders the document item header', () => {
        renderRow();

        expect(screen.getByTestId('document-item-doc-1')).toBeInTheDocument();
    });

    it('renders fetched chunks when data is available', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunksQuery.mockReturnValue({
            data: {
                knowledgeBaseDocumentChunks: [
                    {content: 'A', id: 'chunk-2', knowledgeBaseDocumentId: 'doc-1', metadata: null},
                    {content: 'B', id: 'chunk-1', knowledgeBaseDocumentId: 'doc-1', metadata: null},
                ],
            },
            isLoading: false,
        });

        renderRow();

        expect(screen.getByTestId('chunk-list-Document 1')).toHaveTextContent('2 chunks');
    });

    it('shows a loading indicator while chunks are fetching', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunksQuery.mockReturnValue({data: undefined, isLoading: true});

        renderRow();

        expect(screen.getByText('Loading chunks...')).toBeInTheDocument();
    });
});
