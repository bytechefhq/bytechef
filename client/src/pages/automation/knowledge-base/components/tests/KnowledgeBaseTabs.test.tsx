import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseTabs from '../KnowledgeBaseTabs';

vi.mock('@/components/ui/tabs', () => ({
    Tabs: ({children, defaultValue}: {children: React.ReactNode; className?: string; defaultValue: string}) => (
        <div data-default-value={defaultValue} data-testid="tabs">
            {children}
        </div>
    ),
    TabsContent: ({children, value}: {children: React.ReactNode; className?: string; value: string}) => (
        <div data-testid={`tabs-content-${value}`}>{children}</div>
    ),
    TabsList: ({children}: {children: React.ReactNode}) => <div data-testid="tabs-list">{children}</div>,
    TabsTrigger: ({children, value}: {children: React.ReactNode; value: string}) => (
        <button data-testid={`tabs-trigger-${value}`}>{children}</button>
    ),
}));

vi.mock('../KnowledgeBaseSearchInterface', () => ({
    default: ({knowledgeBaseId}: {knowledgeBaseId: string}) => (
        <div data-testid={`search-interface-${knowledgeBaseId}`}>Search Interface</div>
    ),
}));

vi.mock('../UploadKnowledgeBaseDocumentDialog', () => ({
    default: ({knowledgeBaseId}: {knowledgeBaseId: string}) => (
        <div data-testid={`upload-dialog-${knowledgeBaseId}`}>Upload Dialog</div>
    ),
}));

vi.mock('../knowledge-base-document-list/KnowledgeBaseDocumentList', () => ({
    default: ({documents, knowledgeBaseId}: {documents: unknown[]; knowledgeBaseId: string}) => (
        <div data-testid={`document-list-${knowledgeBaseId}`}>Document List ({documents.length} docs)</div>
    ),
}));

const mockDocuments = [
    {id: 'doc-1', name: 'Document 1', status: 1},
    {id: 'doc-2', name: 'Document 2', status: 1},
];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseTabs', () => {
    it('renders tabs container', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('tabs')).toBeInTheDocument();
    });

    it('has documents as default tab', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('tabs')).toHaveAttribute('data-default-value', 'documents');
    });

    it('renders Documents tab trigger', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('tabs-trigger-documents')).toBeInTheDocument();
        expect(screen.getByText('Documents')).toBeInTheDocument();
    });

    it('renders Search tab trigger', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('tabs-trigger-search')).toBeInTheDocument();
        expect(screen.getByText('Search')).toBeInTheDocument();
    });

    it('renders upload document dialog', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('upload-dialog-kb-1')).toBeInTheDocument();
    });

    it('renders document list in documents tab', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('document-list-kb-1')).toBeInTheDocument();
    });

    it('passes documents to document list', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Document List (2 docs)')).toBeInTheDocument();
    });

    it('renders search interface in search tab', () => {
        render(<KnowledgeBaseTabs documents={mockDocuments} knowledgeBaseId="kb-1" />);

        expect(screen.getByTestId('search-interface-kb-1')).toBeInTheDocument();
    });
});
