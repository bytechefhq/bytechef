import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentListItemDropdownMenu from '../KnowledgeBaseDocumentListItemDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        handleDelete: vi.fn(),
        mockUseKnowledgeBaseDocumentListItemDropdownMenu: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentListItemDropdownMenu', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentListItemDropdownMenu,
}));

const defaultMockReturn = {
    handleDelete: hoisted.handleDelete,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentListItemDropdownMenu.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (documentId = 'doc-1') => {
    return render(<KnowledgeBaseDocumentListItemDropdownMenu documentId={documentId} />);
};

describe('KnowledgeBaseDocumentListItemDropdownMenu', () => {
    it('renders the dropdown trigger button', () => {
        renderComponent();

        expect(screen.getByRole('button', {name: 'More Document Actions'})).toBeInTheDocument();
    });

    it('opens dropdown menu when clicking trigger', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Document Actions'});
        await userEvent.click(trigger);

        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('calls handleDelete when clicking Delete menu item', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Document Actions'});
        await userEvent.click(trigger);

        const deleteItem = screen.getByText('Delete');
        await userEvent.click(deleteItem);

        expect(hoisted.handleDelete).toHaveBeenCalledTimes(1);
    });

    it('passes correct documentId to hook', () => {
        renderComponent('custom-doc-id');

        expect(hoisted.mockUseKnowledgeBaseDocumentListItemDropdownMenu).toHaveBeenCalledWith({
            documentId: 'custom-doc-id',
        });
    });

    it('stops event propagation on trigger click', async () => {
        const parentClickHandler = vi.fn();

        render(
            <div onClick={parentClickHandler}>
                <KnowledgeBaseDocumentListItemDropdownMenu documentId="doc-1" />
            </div>
        );

        const trigger = screen.getByRole('button', {name: 'More Document Actions'});
        await userEvent.click(trigger);

        // The trigger has stopPropagation, so parent should not be called
        expect(parentClickHandler).not.toHaveBeenCalled();
    });
});

describe('KnowledgeBaseDocumentListItemDropdownMenu styling', () => {
    it('Delete menu item has destructive styling', async () => {
        renderComponent();

        const trigger = screen.getByRole('button', {name: 'More Document Actions'});
        await userEvent.click(trigger);

        const deleteMenuItem = screen.getByText('Delete').closest('[class*="dropdown-menu-item-destructive"]');

        expect(deleteMenuItem).toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentListItemDropdownMenu with different document IDs', () => {
    it('works with numeric-like document IDs', () => {
        renderComponent('123');

        expect(hoisted.mockUseKnowledgeBaseDocumentListItemDropdownMenu).toHaveBeenCalledWith({
            documentId: '123',
        });
    });

    it('works with UUID-like document IDs', () => {
        renderComponent('550e8400-e29b-41d4-a716-446655440000');

        expect(hoisted.mockUseKnowledgeBaseDocumentListItemDropdownMenu).toHaveBeenCalledWith({
            documentId: '550e8400-e29b-41d4-a716-446655440000',
        });
    });
});
