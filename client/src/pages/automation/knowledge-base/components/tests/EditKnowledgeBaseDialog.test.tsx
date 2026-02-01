import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import EditKnowledgeBaseDialog from '../EditKnowledgeBaseDialog';

const hoisted = vi.hoisted(() => {
    return {
        handleCancel: vi.fn(),
        handleDescriptionChange: vi.fn(),
        handleNameChange: vi.fn(),
        handleOpenChange: vi.fn(),
        handleSave: vi.fn(),
        mockUseEditKnowledgeBaseDialog: vi.fn(),
    };
});

vi.mock('../hooks/useEditKnowledgeBaseDialog', () => ({
    default: hoisted.mockUseEditKnowledgeBaseDialog,
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({
        children,
        disabled,
        icon,
        onClick,
        variant,
    }: {
        children?: React.ReactNode;
        disabled?: boolean;
        icon?: React.ReactNode;
        onClick?: () => void;
        size?: string;
        variant?: string;
    }) => (
        <button data-testid={`button-${variant || 'default'}`} disabled={disabled} onClick={onClick}>
            {icon}

            {children}
        </button>
    ),
}));

const mockKnowledgeBase = {
    description: 'Test description',
    id: 'kb-1',
    maxChunkSize: 1024,
    minChunkSizeChars: 1,
    name: 'Test KB',
    overlap: 200,
};

const defaultMockReturn = {
    canSubmit: true,
    description: 'Test description',
    handleCancel: hoisted.handleCancel,
    handleDescriptionChange: hoisted.handleDescriptionChange,
    handleNameChange: hoisted.handleNameChange,
    handleOpenChange: hoisted.handleOpenChange,
    handleSave: hoisted.handleSave,
    isPending: false,
    name: 'Test KB',
    open: true,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseEditKnowledgeBaseDialog.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = (props = {}) => {
    return render(<EditKnowledgeBaseDialog knowledgeBase={mockKnowledgeBase} {...props} />);
};

describe('EditKnowledgeBaseDialog', () => {
    it('renders dialog title', () => {
        renderComponent();

        expect(screen.getByText('Edit Knowledge Base')).toBeInTheDocument();
    });

    it('renders dialog description', () => {
        renderComponent();

        expect(screen.getByText('Update the general settings for this knowledge base.')).toBeInTheDocument();
    });

    it('renders name input with value', () => {
        renderComponent();

        const nameInput = screen.getByPlaceholderText('Knowledge base name');

        expect(nameInput).toHaveValue('Test KB');
    });

    it('renders description textarea with value', () => {
        renderComponent();

        const descriptionInput = screen.getByPlaceholderText('Describe this knowledge base (optional)');

        expect(descriptionInput).toHaveValue('Test description');
    });

    it('calls handleNameChange when name input changes', async () => {
        renderComponent();

        const nameInput = screen.getByPlaceholderText('Knowledge base name');
        await userEvent.clear(nameInput);
        await userEvent.type(nameInput, 'N');

        expect(hoisted.handleNameChange).toHaveBeenCalled();
    });

    it('calls handleDescriptionChange when description changes', async () => {
        renderComponent();

        const descriptionInput = screen.getByPlaceholderText('Describe this knowledge base (optional)');
        await userEvent.type(descriptionInput, 'New');

        expect(hoisted.handleDescriptionChange).toHaveBeenCalled();
    });

    it('calls handleCancel when Cancel button is clicked', async () => {
        renderComponent();

        const cancelButton = screen.getByText('Cancel');
        await userEvent.click(cancelButton);

        expect(hoisted.handleCancel).toHaveBeenCalled();
    });

    it('calls handleSave when Save button is clicked', async () => {
        renderComponent();

        const saveButton = screen.getByText('Save');
        await userEvent.click(saveButton);

        expect(hoisted.handleSave).toHaveBeenCalled();
    });

    it('disables Save button when canSubmit is false', () => {
        hoisted.mockUseEditKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            canSubmit: false,
        });

        renderComponent();

        const saveButton = screen.getByText('Save');

        expect(saveButton).toBeDisabled();
    });

    it('disables Save button when isPending is true', () => {
        hoisted.mockUseEditKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            isPending: true,
        });

        renderComponent();

        const saveButton = screen.getByText('Saving...');

        expect(saveButton).toBeDisabled();
    });

    it('shows Saving... text when isPending is true', () => {
        hoisted.mockUseEditKnowledgeBaseDialog.mockReturnValue({
            ...defaultMockReturn,
            isPending: true,
        });

        renderComponent();

        expect(screen.getByText('Saving...')).toBeInTheDocument();
    });

    it('passes correct props to hook', () => {
        const mockOnOpenChange = vi.fn();

        renderComponent({onOpenChange: mockOnOpenChange, open: true});

        expect(hoisted.mockUseEditKnowledgeBaseDialog).toHaveBeenCalledWith({
            knowledgeBase: mockKnowledgeBase,
            onOpenChange: mockOnOpenChange,
            open: true,
        });
    });
});
