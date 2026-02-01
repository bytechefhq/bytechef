import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDeleteAlertDialog from '../KnowledgeBaseDeleteAlertDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleCancelClick: vi.fn(),
        handleDeleteClick: vi.fn(),
        mockUseKnowledgeBaseDeleteAlertDialog: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDeleteAlertDialog', () => ({
    default: hoisted.mockUseKnowledgeBaseDeleteAlertDialog,
}));

vi.mock('@/components/DeleteAlertDialog', () => ({
    default: ({onCancel, onDelete, open}: {onCancel: () => void; onDelete: () => void; open: boolean}) =>
        open ? (
            <div data-testid="delete-alert-dialog">
                <button data-testid="cancel-button" onClick={onCancel}>
                    Cancel
                </button>

                <button data-testid="delete-button" onClick={onDelete}>
                    Delete
                </button>
            </div>
        ) : null,
}));

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDeleteAlertDialog.mockReturnValue({
        handleCancelClick: hoisted.handleCancelClick,
        handleDeleteClick: hoisted.handleDeleteClick,
    });
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const mockOnClose = vi.fn();

const renderComponent = (props = {}) => {
    return render(
        <KnowledgeBaseDeleteAlertDialog knowledgeBaseId="kb-1" onClose={mockOnClose} open={true} {...props} />
    );
};

describe('KnowledgeBaseDeleteAlertDialog', () => {
    it('renders dialog when open', () => {
        renderComponent();

        expect(screen.getByTestId('delete-alert-dialog')).toBeInTheDocument();
    });

    it('does not render dialog when closed', () => {
        renderComponent({open: false});

        expect(screen.queryByTestId('delete-alert-dialog')).not.toBeInTheDocument();
    });

    it('calls handleCancelClick when Cancel is clicked', async () => {
        renderComponent();

        const cancelButton = screen.getByTestId('cancel-button');
        await userEvent.click(cancelButton);

        expect(hoisted.handleCancelClick).toHaveBeenCalled();
    });

    it('calls handleDeleteClick when Delete is clicked', async () => {
        renderComponent();

        const deleteButton = screen.getByTestId('delete-button');
        await userEvent.click(deleteButton);

        expect(hoisted.handleDeleteClick).toHaveBeenCalled();
    });

    it('passes correct props to hook', () => {
        renderComponent();

        expect(hoisted.mockUseKnowledgeBaseDeleteAlertDialog).toHaveBeenCalledWith({
            knowledgeBaseId: 'kb-1',
            onClose: mockOnClose,
        });
    });
});
