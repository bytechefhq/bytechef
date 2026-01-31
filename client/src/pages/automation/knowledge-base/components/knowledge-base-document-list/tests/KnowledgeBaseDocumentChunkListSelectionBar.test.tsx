import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentChunkListSelectionBar from '../KnowledgeBaseDocumentChunkListSelectionBar';

const hoisted = vi.hoisted(() => {
    return {
        handleClearSelection: vi.fn(),
        handleDeleteSelected: vi.fn(),
        mockUseKnowledgeBaseDocumentChunkListSelectionBar: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentChunkListSelectionBar', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentChunkListSelectionBar,
}));

const defaultMockReturn = {
    handleClearSelection: hoisted.handleClearSelection,
    handleDeleteSelected: hoisted.handleDeleteSelected,
    hasSelection: true,
    selectedCount: 2,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentChunkListSelectionBar.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = () => {
    return render(<KnowledgeBaseDocumentChunkListSelectionBar />);
};

describe('KnowledgeBaseDocumentChunkListSelectionBar', () => {
    it('renders when there is a selection', () => {
        renderComponent();

        expect(screen.getByText('2 chunk(s) selected')).toBeInTheDocument();
    });

    it('does not render when there is no selection', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkListSelectionBar.mockReturnValue({
            ...defaultMockReturn,
            hasSelection: false,
            selectedCount: 0,
        });

        renderComponent();

        expect(screen.queryByText(/selected/)).not.toBeInTheDocument();
    });

    it('displays correct count for single selection', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkListSelectionBar.mockReturnValue({
            ...defaultMockReturn,
            selectedCount: 1,
        });

        renderComponent();

        expect(screen.getByText('1 chunk(s) selected')).toBeInTheDocument();
    });

    it('displays correct count for multiple selections', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkListSelectionBar.mockReturnValue({
            ...defaultMockReturn,
            selectedCount: 5,
        });

        renderComponent();

        expect(screen.getByText('5 chunk(s) selected')).toBeInTheDocument();
    });

    it('renders Delete Selected button', () => {
        renderComponent();

        expect(screen.getByRole('button', {name: /Delete Selected/})).toBeInTheDocument();
    });

    it('renders Clear Selection button', () => {
        renderComponent();

        expect(screen.getByRole('button', {name: 'Clear Selection'})).toBeInTheDocument();
    });

    it('calls handleDeleteSelected when clicking Delete Selected button', async () => {
        renderComponent();

        const deleteButton = screen.getByRole('button', {name: /Delete Selected/});
        await userEvent.click(deleteButton);

        expect(hoisted.handleDeleteSelected).toHaveBeenCalledTimes(1);
    });

    it('calls handleClearSelection when clicking Clear Selection button', async () => {
        renderComponent();

        const clearButton = screen.getByRole('button', {name: 'Clear Selection'});
        await userEvent.click(clearButton);

        expect(hoisted.handleClearSelection).toHaveBeenCalledTimes(1);
    });
});

describe('KnowledgeBaseDocumentChunkListSelectionBar styling', () => {
    it('Delete Selected button has destructive variant', () => {
        renderComponent();

        const deleteButton = screen.getByRole('button', {name: /Delete Selected/});

        expect(deleteButton).toBeInTheDocument();
    });

    it('Clear Selection button has outline variant', () => {
        renderComponent();

        const clearButton = screen.getByRole('button', {name: 'Clear Selection'});

        expect(clearButton).toBeInTheDocument();
    });
});
