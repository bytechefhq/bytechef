import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDocumentChunkEditDialog from '../KnowledgeBaseDocumentChunkEditDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleClose: vi.fn(),
        handleContentChange: vi.fn(),
        handleOpenChange: vi.fn(),
        handleSave: vi.fn(),
        mockUseKnowledgeBaseDocumentChunkEditDialog: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDocumentChunkEditDialog', () => ({
    default: hoisted.mockUseKnowledgeBaseDocumentChunkEditDialog,
}));

const defaultMockReturn = {
    content: 'Test chunk content',
    handleClose: hoisted.handleClose,
    handleContentChange: hoisted.handleContentChange,
    handleOpen: vi.fn(),
    handleOpenChange: hoisted.handleOpenChange,
    handleSave: hoisted.handleSave,
    isPending: false,
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDocumentChunkEditDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = () => {
    return render(<KnowledgeBaseDocumentChunkEditDialog knowledgeBaseId="kb-1" />);
};

describe('KnowledgeBaseDocumentChunkEditDialog', () => {
    it('renders the dialog when open is true', () => {
        renderComponent();

        expect(screen.getByText('Edit Chunk')).toBeInTheDocument();
    });

    it('displays the dialog description', () => {
        renderComponent();

        expect(
            screen.getByText('Modify the text content of this chunk. Changes will be re-embedded in the vector store.')
        ).toBeInTheDocument();
    });

    it('renders content textarea with current content', () => {
        renderComponent();

        const textarea = screen.getByRole('textbox');

        expect(textarea).toHaveValue('Test chunk content');
    });

    it('renders Cancel and Save buttons', () => {
        renderComponent();

        expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Save Changes'})).toBeInTheDocument();
    });

    it('calls handleSave when clicking Save Changes button', async () => {
        renderComponent();

        const saveButton = screen.getByRole('button', {name: 'Save Changes'});
        await userEvent.click(saveButton);

        expect(hoisted.handleSave).toHaveBeenCalledTimes(1);
    });

    it('calls handleClose when clicking Cancel button', async () => {
        renderComponent();

        const cancelButton = screen.getByRole('button', {name: 'Cancel'});
        await userEvent.click(cancelButton);

        expect(hoisted.handleClose).toHaveBeenCalledTimes(1);
    });

    it('calls handleContentChange when typing in textarea', async () => {
        renderComponent();

        const textarea = screen.getByRole('textbox');
        await userEvent.type(textarea, ' new text');

        expect(hoisted.handleContentChange).toHaveBeenCalled();
    });

    it('disables Save button when isPending is true', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkEditDialog.mockReturnValue({
            ...defaultMockReturn,
            isPending: true,
        });

        renderComponent();

        const saveButton = screen.getByRole('button', {name: 'Saving...'});

        expect(saveButton).toBeDisabled();
    });

    it('shows Saving... text when isPending is true', () => {
        hoisted.mockUseKnowledgeBaseDocumentChunkEditDialog.mockReturnValue({
            ...defaultMockReturn,
            isPending: true,
        });

        renderComponent();

        expect(screen.getByRole('button', {name: 'Saving...'})).toBeInTheDocument();
    });

    it('renders Content label', () => {
        renderComponent();

        expect(screen.getByText('Content')).toBeInTheDocument();
    });
});

describe('KnowledgeBaseDocumentChunkEditDialog closed state', () => {
    beforeEach(() => {
        hoisted.mockUseKnowledgeBaseDocumentChunkEditDialog.mockReturnValue({
            ...defaultMockReturn,
            open: false,
        });
    });

    it('does not render the dialog content when open is false', () => {
        renderComponent();

        expect(screen.queryByText('Edit Chunk')).not.toBeInTheDocument();
    });
});
