import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseListItemDeleteDialog from '../KnowledgeBaseListItemDeleteDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleCancelClick: vi.fn(),
        handleDeleteClick: vi.fn(),
        mockUseKnowledgeBaseListItemDeleteDialog: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseListItemDeleteDialog', () => ({
    default: hoisted.mockUseKnowledgeBaseListItemDeleteDialog,
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseListItemDeleteDialog.mockReturnValue({
        handleCancelClick: hoisted.handleCancelClick,
        handleDeleteClick: hoisted.handleDeleteClick,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const mockOnClose = vi.fn();

describe('KnowledgeBaseListItemDeleteDialog', () => {
    it('renders dialog when open', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    });

    it('does not render dialog when closed', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={false} />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('renders title', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByRole('heading', {level: 2})).toHaveTextContent('Are you absolutely sure?');
    });

    it('renders description', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByRole('alertdialog')).toHaveAccessibleDescription(
            'This action cannot be undone. This will permanently delete the knowledge base and all documents it contains.'
        );
    });

    it('renders Cancel button', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
    });

    it('renders Delete button', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
    });

    it('calls handleCancelClick when Cancel is clicked', async () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.handleCancelClick).toHaveBeenCalled();
    });

    it('calls handleCancelClick when the close button is clicked', async () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(hoisted.handleCancelClick).toHaveBeenCalled();
    });

    it('calls handleDeleteClick when Delete is clicked', async () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.handleDeleteClick).toHaveBeenCalled();
    });

    it('passes correct props to hook', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(hoisted.mockUseKnowledgeBaseListItemDeleteDialog).toHaveBeenCalledWith({
            knowledgeBaseId: 'kb-1',
            onClose: mockOnClose,
        });
    });
});
