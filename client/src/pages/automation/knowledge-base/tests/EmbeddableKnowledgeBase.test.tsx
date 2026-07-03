import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import EmbeddableKnowledgeBase from '../EmbeddableKnowledgeBase';

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useKnowledgeBaseQuery: vi.fn(),
    };
});

vi.mock('@/components/PageLoader', () => ({
    default: ({children, errors, loading}: {children: React.ReactNode; errors: unknown[]; loading: boolean}) =>
        loading ? (
            <div data-testid="page-loader-loading">Loading...</div>
        ) : errors.filter(Boolean).length > 0 ? (
            <div data-testid="page-loader-error">Error</div>
        ) : (
            <div data-testid="page-loader-content">{children}</div>
        ),
}));

vi.mock('../components/KnowledgeBaseInfoCard', () => ({
    default: ({knowledgeBase}: {knowledgeBase: {name: string}}) => (
        <div data-testid="knowledge-base-info-card">{knowledgeBase.name}</div>
    ),
}));

vi.mock('../components/KnowledgeBaseTabs', () => ({
    default: ({documents, knowledgeBaseId}: {documents: unknown[]; knowledgeBaseId: string}) => (
        <div data-testid="knowledge-base-tabs">
            Tabs for {knowledgeBaseId} ({documents.length} docs)
        </div>
    ),
}));

const {useKnowledgeBaseQuery} = await import('@/shared/middleware/graphql');

const mockUseKnowledgeBaseQuery = vi.mocked(useKnowledgeBaseQuery);

const MOCK_KB = {
    createdDate: null,
    description: 'A test knowledge base',
    documents: [
        {chunks: [], createdDate: null, id: 'doc-1', name: 'guide.pdf', status: 2, tags: []},
        {chunks: [], createdDate: null, id: 'doc-2', name: 'notes.txt', status: 2, tags: []},
    ],
    id: 'kb-42',
    lastModifiedDate: null,
    maxChunkSize: 1024,
    minChunkSizeChars: 100,
    name: 'My Knowledge Base',
    overlap: 50,
};

describe('EmbeddableKnowledgeBase', () => {
    it('renders KnowledgeBaseInfoCard and KnowledgeBaseTabs when data is loaded', () => {
        mockUseKnowledgeBaseQuery.mockReturnValue({
            data: {knowledgeBase: MOCK_KB},
            error: undefined,
            isLoading: false,
        } as unknown as ReturnType<typeof useKnowledgeBaseQuery>);

        render(<EmbeddableKnowledgeBase knowledgeBaseId="kb-42" />);

        expect(screen.getByTestId('knowledge-base-info-card')).toBeInTheDocument();
        expect(screen.getByTestId('knowledge-base-info-card')).toHaveTextContent('My Knowledge Base');
        expect(screen.getByTestId('knowledge-base-tabs')).toBeInTheDocument();
        expect(screen.getByTestId('knowledge-base-tabs')).toHaveTextContent('Tabs for kb-42 (2 docs)');
    });

    it('shows loading state while fetching', () => {
        mockUseKnowledgeBaseQuery.mockReturnValue({
            data: undefined,
            error: undefined,
            isLoading: true,
        } as unknown as ReturnType<typeof useKnowledgeBaseQuery>);

        render(<EmbeddableKnowledgeBase knowledgeBaseId="kb-42" />);

        expect(screen.getByTestId('page-loader-loading')).toBeInTheDocument();
    });

    it('shows error state on fetch failure', () => {
        mockUseKnowledgeBaseQuery.mockReturnValue({
            data: undefined,
            error: new Error('Network error'),
            isLoading: false,
        } as unknown as ReturnType<typeof useKnowledgeBaseQuery>);

        render(<EmbeddableKnowledgeBase knowledgeBaseId="kb-42" />);

        expect(screen.getByTestId('page-loader-error')).toBeInTheDocument();
    });

    it('renders nothing inside content when knowledgeBase is null', () => {
        mockUseKnowledgeBaseQuery.mockReturnValue({
            data: {knowledgeBase: null},
            error: undefined,
            isLoading: false,
        } as unknown as ReturnType<typeof useKnowledgeBaseQuery>);

        render(<EmbeddableKnowledgeBase knowledgeBaseId="kb-42" />);

        expect(screen.getByTestId('page-loader-content')).toBeInTheDocument();
        expect(screen.queryByTestId('knowledge-base-info-card')).not.toBeInTheDocument();
        expect(screen.queryByTestId('knowledge-base-tabs')).not.toBeInTheDocument();
    });

    it('passes filtered non-null documents to KnowledgeBaseTabs', () => {
        mockUseKnowledgeBaseQuery.mockReturnValue({
            data: {
                knowledgeBase: {
                    ...MOCK_KB,
                    documents: [
                        {chunks: [], createdDate: null, id: 'doc-1', name: 'guide.pdf', status: 2, tags: []},
                        null,
                        {chunks: [], createdDate: null, id: 'doc-3', name: 'faq.md', status: 2, tags: []},
                    ],
                },
            },
            error: undefined,
            isLoading: false,
        } as unknown as ReturnType<typeof useKnowledgeBaseQuery>);

        render(<EmbeddableKnowledgeBase knowledgeBaseId="kb-42" />);

        expect(screen.getByTestId('knowledge-base-tabs')).toHaveTextContent('Tabs for kb-42 (2 docs)');
    });
});
