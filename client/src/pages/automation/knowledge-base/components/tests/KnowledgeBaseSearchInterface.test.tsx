import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseSearchInterface from '../KnowledgeBaseSearchInterface';

const hoisted = vi.hoisted(() => {
    return {
        handleClearSearch: vi.fn(),
        handleSearch: vi.fn(),
        mockUseKnowledgeBaseSearchInterface: vi.fn(),
        setMetadataFilters: vi.fn(),
        setQuery: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseSearchInterface', () => ({
    default: hoisted.mockUseKnowledgeBaseSearchInterface,
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({
        children,
        disabled,
        onClick,
        type,
    }: {
        children?: React.ReactNode;
        disabled?: boolean;
        onClick?: () => void;
        size?: string;
        type?: 'button' | 'submit' | 'reset';
        variant?: string;
    }) => (
        <button data-testid="button" disabled={disabled} onClick={onClick} type={type || 'button'}>
            {children}
        </button>
    ),
}));

const defaultMockReturn = {
    canSearch: true,
    handleClearSearch: hoisted.handleClearSearch,
    handleSearch: hoisted.handleSearch,
    isLoading: false,
    metadataFilters: '',
    query: '',
    results: [],
    searchQuery: '',
    setMetadataFilters: hoisted.setMetadataFilters,
    setQuery: hoisted.setQuery,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseSearchInterface', () => {
    it('renders search form', () => {
        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Search Query')).toBeInTheDocument();
    });

    it('renders query input', () => {
        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByPlaceholderText('Enter your search query...')).toBeInTheDocument();
    });

    it('renders metadata filters input', () => {
        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByPlaceholderText('{"category": "documentation"}')).toBeInTheDocument();
    });

    it('calls setQuery when query input changes', async () => {
        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        const queryInput = screen.getByPlaceholderText('Enter your search query...');
        await userEvent.type(queryInput, 'test');

        expect(hoisted.setQuery).toHaveBeenCalled();
    });

    it('calls setMetadataFilters when metadata input changes', async () => {
        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        const filtersInput = screen.getByPlaceholderText('{"category": "documentation"}');
        await userEvent.type(filtersInput, 'test');

        expect(hoisted.setMetadataFilters).toHaveBeenCalled();
    });

    it('disables search button when canSearch is false', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            canSearch: false,
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        const searchButton = screen.getAllByTestId('button')[0];

        expect(searchButton).toBeDisabled();
    });

    it('shows loading text when isLoading', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            isLoading: true,
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Searching...')).toBeInTheDocument();
    });

    it('shows result count when search has been performed', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            results: [{content: 'Result', id: 'r-1', metadata: null, score: 0.9}],
            searchQuery: 'test',
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Found 1 result')).toBeInTheDocument();
    });

    it('shows plural results text for multiple results', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            results: [
                {content: 'Result 1', id: 'r-1', metadata: null, score: 0.9},
                {content: 'Result 2', id: 'r-2', metadata: null, score: 0.8},
            ],
            searchQuery: 'test',
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Found 2 results')).toBeInTheDocument();
    });

    it('shows no results message when search returns empty', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            results: [],
            searchQuery: 'test',
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('No results found for your query.')).toBeInTheDocument();
    });

    it('renders search results', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            results: [{content: 'Test content', id: 'r-1', metadata: {file_name: 'doc.pdf'}, score: 0.95}],
            searchQuery: 'test',
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Test content')).toBeInTheDocument();
        expect(screen.getByText('doc.pdf')).toBeInTheDocument();
    });

    it('displays score percentage', () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            results: [{content: 'Test content', id: 'r-1', metadata: {file_name: 'doc.pdf'}, score: 0.95}],
            searchQuery: 'test',
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        expect(screen.getByText('Score: 95.0%')).toBeInTheDocument();
    });

    it('calls handleClearSearch when Clear button is clicked', async () => {
        hoisted.mockUseKnowledgeBaseSearchInterface.mockReturnValue({
            ...defaultMockReturn,
            results: [],
            searchQuery: 'test',
        });

        render(<KnowledgeBaseSearchInterface knowledgeBaseId="kb-1" />);

        const clearButton = screen.getByText('Clear Search');
        await userEvent.click(clearButton);

        expect(hoisted.handleClearSearch).toHaveBeenCalled();
    });
});
