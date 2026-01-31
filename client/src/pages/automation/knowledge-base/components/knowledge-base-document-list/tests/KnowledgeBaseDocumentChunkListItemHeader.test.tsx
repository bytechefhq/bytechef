import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentChunkListItemHeader from '../KnowledgeBaseDocumentChunkListItemHeader';

const hoisted = vi.hoisted(() => {
    return {
        handleSelectionChange: vi.fn(),
        mockUseKnowledgeBaseDocumentChunkListItemHeader: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentChunkListItemHeader', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentChunkListItemHeader,
}));

const defaultMockReturn = {
    handleSelectionChange: hoisted.handleSelectionChange,
    isSelected: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentChunkListItemHeader.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (props = {chunkId: 'chunk-1', chunkIndex: 0, documentName: 'Test Document'}) => {
    return render(<KnowledgeBaseDocumentChunkListItemHeader {...props} />);
};

describe('KnowledgeBaseDocumentChunkListItemHeader', () => {
    it('renders the checkbox', () => {
        renderComponent();

        expect(screen.getByRole('checkbox')).toBeInTheDocument();
    });

    it('renders the document name', () => {
        renderComponent({chunkId: 'chunk-1', chunkIndex: 0, documentName: 'My Document'});

        expect(screen.getByText('My Document')).toBeInTheDocument();
    });

    it('renders the chunk number with 1-based index', () => {
        renderComponent({chunkId: 'chunk-1', chunkIndex: 0, documentName: 'Test Document'});

        expect(screen.getByText('Chunk 1')).toBeInTheDocument();
    });

    it('renders correct chunk number for different indices', () => {
        renderComponent({chunkId: 'chunk-5', chunkIndex: 4, documentName: 'Test Document'});

        expect(screen.getByText('Chunk 5')).toBeInTheDocument();
    });

    it('checkbox is unchecked when not selected', () => {
        renderComponent();

        const checkbox = screen.getByRole('checkbox');

        expect(checkbox).not.toBeChecked();
    });

    it('checkbox is checked when selected', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkListItemHeader.mockReturnValue({
            ...defaultMockReturn,
            isSelected: true,
        });

        renderComponent();

        const checkbox = screen.getByRole('checkbox');

        expect(checkbox).toBeChecked();
    });

    it('calls handleSelectionChange when checkbox is clicked', async () => {
        renderComponent();

        const checkbox = screen.getByRole('checkbox');
        await userEvent.click(checkbox);

        expect(hoisted.handleSelectionChange).toHaveBeenCalledTimes(1);
    });

    it('passes correct chunkId to hook', () => {
        renderComponent({chunkId: 'specific-chunk', chunkIndex: 2, documentName: 'Doc'});

        expect(hoisted.mockUseKnowledgeBaseDocumentChunkListItemHeader).toHaveBeenCalledWith({
            chunkId: 'specific-chunk',
        });
    });

    it('renders separator dot between document name and chunk number', () => {
        renderComponent();

        expect(screen.getByText('•')).toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentChunkListItemHeader layout', () => {
    it('renders document name before chunk number', () => {
        renderComponent({chunkId: 'chunk-1', chunkIndex: 2, documentName: 'First Doc'});

        expect(screen.getByText('First Doc')).toBeInTheDocument();
        expect(screen.getByText('Chunk 3')).toBeInTheDocument();
    });
});
