import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseInfoCard from '../KnowledgeBaseInfoCard';

vi.mock('../KnowledgeBaseDropdownMenu', () => ({
    default: ({knowledgeBase}: {knowledgeBase: {id: string}}) => (
        <div data-testid={`dropdown-menu-${knowledgeBase.id}`}>Dropdown</div>
    ),
}));

const mockKnowledgeBase = {
    description: 'Test description',
    documents: [
        {id: 'doc-1', name: 'Document 1', status: 1},
        {id: 'doc-2', name: 'Document 2', status: 1},
    ],
    id: 'kb-1',
    maxChunkSize: 1024,
    minChunkSizeChars: 100,
    name: 'Test KB',
    overlap: 200,
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseInfoCard', () => {
    it('renders knowledge base name', () => {
        render(<KnowledgeBaseInfoCard knowledgeBase={mockKnowledgeBase} />);

        expect(screen.getByText('Test KB')).toBeInTheDocument();
    });

    it('renders description when present', () => {
        render(<KnowledgeBaseInfoCard knowledgeBase={mockKnowledgeBase} />);

        expect(screen.getByText('Test description')).toBeInTheDocument();
    });

    it('does not render description when empty', () => {
        const kbWithoutDescription = {...mockKnowledgeBase, description: ''};

        render(<KnowledgeBaseInfoCard knowledgeBase={kbWithoutDescription} />);

        expect(screen.queryByText('Test description')).not.toBeInTheDocument();
    });

    it('renders document count', () => {
        render(<KnowledgeBaseInfoCard knowledgeBase={mockKnowledgeBase} />);

        expect(screen.getByText('Documents:')).toBeInTheDocument();
        expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('renders chunk size range', () => {
        render(<KnowledgeBaseInfoCard knowledgeBase={mockKnowledgeBase} />);

        expect(screen.getByText('Chunk Size:')).toBeInTheDocument();
        expect(screen.getByText('100-1024')).toBeInTheDocument();
    });

    it('renders overlap value', () => {
        render(<KnowledgeBaseInfoCard knowledgeBase={mockKnowledgeBase} />);

        expect(screen.getByText('Overlap:')).toBeInTheDocument();
        expect(screen.getByText('200')).toBeInTheDocument();
    });

    it('renders dropdown menu', () => {
        render(<KnowledgeBaseInfoCard knowledgeBase={mockKnowledgeBase} />);

        expect(screen.getByTestId('dropdown-menu-kb-1')).toBeInTheDocument();
    });

    it('handles empty documents array', () => {
        const kbWithNoDocuments = {...mockKnowledgeBase, documents: []};

        render(<KnowledgeBaseInfoCard knowledgeBase={kbWithNoDocuments} />);

        expect(screen.getByText('0')).toBeInTheDocument();
    });
});
