import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentChunkListItemDropdownMenu from '../KnowledgeBaseDocumentChunkListItemDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        handleDelete: vi.fn(),
        handleEdit: vi.fn(),
        mockUseKnowledgeBaseDocumentChunkListItemDropdownMenu: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentChunkListItemDropdownMenu', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentChunkListItemDropdownMenu,
}));

const mockChunk: KnowledgeBaseDocumentChunk = {
    content: 'Test chunk content',
    id: 'chunk-1',
    knowledgeBaseDocumentId: 'doc-1',
    metadata: null,
};

const defaultMockReturn = {
    handleDelete: hoisted.handleDelete,
    handleEdit: hoisted.handleEdit,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentChunkListItemDropdownMenu.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (chunk: KnowledgeBaseDocumentChunk = mockChunk) => {
    return render(<KnowledgeBaseDocumentChunkListItemDropdownMenu chunk={chunk} />);
};

describe('KnowledgeBaseDocumentChunkListItemDropdownMenu', () => {
    it('renders the dropdown trigger button', () => {
        renderComponent();

        expect(screen.getByRole('button', {name: 'More Chunk Actions'})).toBeInTheDocument();
    });

    it('opens dropdown menu when clicking trigger', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Chunk Actions'});
        await userEvent.click(trigger);

        expect(screen.getByText('Edit')).toBeInTheDocument();
        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('calls handleEdit when clicking Edit menu item', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Chunk Actions'});
        await userEvent.click(trigger);

        const editItem = screen.getByText('Edit');
        await userEvent.click(editItem);

        expect(hoisted.handleEdit).toHaveBeenCalledTimes(1);
    });

    it('calls handleDelete when clicking Delete menu item', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Chunk Actions'});
        await userEvent.click(trigger);

        const deleteItem = screen.getByText('Delete');
        await userEvent.click(deleteItem);

        expect(hoisted.handleDelete).toHaveBeenCalledTimes(1);
    });

    it('passes correct chunk to hook', () => {
        const customChunk: KnowledgeBaseDocumentChunk = {
            content: 'Custom content',
            id: 'custom-chunk',
            knowledgeBaseDocumentId: 'doc-1',
            metadata: {page: 5},
        };

        renderComponent(customChunk);

        expect(hoisted.mockUseKnowledgeBaseDocumentChunkListItemDropdownMenu).toHaveBeenCalledWith({
            chunk: customChunk,
        });
    });
});

describe('KnowledgeBaseDocumentChunkListItemDropdownMenu menu items', () => {
    it('Edit menu item has edit icon', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Chunk Actions'});
        await userEvent.click(trigger);

        const editMenuItem = screen.getByText('Edit').closest('[class*="dropdown-menu-item"]');

        expect(editMenuItem).toBeInTheDocument();
    });

    it('Delete menu item has destructive styling', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Chunk Actions'});
        await userEvent.click(trigger);

        const deleteMenuItem = screen.getByText('Delete').closest('[class*="dropdown-menu-item-destructive"]');

        expect(deleteMenuItem).toBeInTheDocument();
    });
});
