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

vi.mock('@/components/ui/alert-dialog', () => ({
    AlertDialog: ({children, open}: {children: React.ReactNode; open: boolean}) =>
        open ? <div data-testid="alert-dialog">{children}</div> : null,
    AlertDialogAction: ({children, onClick}: {children: React.ReactNode; className?: string; onClick?: () => void}) => (
        <button data-testid="alert-action" onClick={onClick}>
            {children}
        </button>
    ),
    AlertDialogCancel: ({children, onClick}: {children: React.ReactNode; className?: string; onClick?: () => void}) => (
        <button data-testid="alert-cancel" onClick={onClick}>
            {children}
        </button>
    ),
    AlertDialogContent: ({children}: {children: React.ReactNode}) => <div data-testid="alert-content">{children}</div>,
    AlertDialogDescription: ({children}: {children: React.ReactNode}) => (
        <p data-testid="alert-description">{children}</p>
    ),
    AlertDialogFooter: ({children}: {children: React.ReactNode}) => <div data-testid="alert-footer">{children}</div>,
    AlertDialogHeader: ({children}: {children: React.ReactNode}) => <div data-testid="alert-header">{children}</div>,
    AlertDialogTitle: ({children}: {children: React.ReactNode}) => <h2 data-testid="alert-title">{children}</h2>,
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

        expect(screen.getByTestId('alert-dialog')).toBeInTheDocument();
    });

    it('does not render dialog when closed', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={false} />);

        expect(screen.queryByTestId('alert-dialog')).not.toBeInTheDocument();
    });

    it('renders title', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByTestId('alert-title')).toHaveTextContent('Are you absolutely sure?');
    });

    it('renders description', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByTestId('alert-description')).toHaveTextContent(
            'This action cannot be undone. This will permanently delete the knowledge base and all documents it contains.'
        );
    });

    it('renders Cancel button', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByTestId('alert-cancel')).toHaveTextContent('Cancel');
    });

    it('renders Delete button', () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        expect(screen.getByTestId('alert-action')).toHaveTextContent('Delete');
    });

    it('calls handleCancelClick when Cancel is clicked', async () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        const cancelButton = screen.getByTestId('alert-cancel');
        await userEvent.click(cancelButton);

        expect(hoisted.handleCancelClick).toHaveBeenCalled();
    });

    it('calls handleDeleteClick when Delete is clicked', async () => {
        render(<KnowledgeBaseListItemDeleteDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} />);

        const deleteButton = screen.getByTestId('alert-action');
        await userEvent.click(deleteButton);

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
