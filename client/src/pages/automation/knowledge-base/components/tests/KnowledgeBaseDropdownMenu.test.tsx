import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseDropdownMenu from '../KnowledgeBaseDropdownMenu';

const hoisted = vi.hoisted(() => {
    return {
        handleCloseDeleteDialog: vi.fn(),
        handleCloseEditDialog: vi.fn(),
        handleShowDeleteDialog: vi.fn(),
        handleShowEditDialog: vi.fn(),
        mockUseKnowledgeBaseDropdownMenu: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseDropdownMenu', () => ({
    default: hoisted.mockUseKnowledgeBaseDropdownMenu,
}));

vi.mock('../EditKnowledgeBaseDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="edit-dialog">Edit Dialog</div> : null),
}));

vi.mock('../KnowledgeBaseDeleteAlertDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="delete-dialog">Delete Dialog</div> : null),
}));

vi.mock('@/components/Button/Button', () => ({
    default: ({
        'aria-label': ariaLabel,
        icon,
        onClick,
    }: {
        'aria-label'?: string;
        icon?: React.ReactNode;
        onClick?: () => void;
        size?: string;
        variant?: string;
    }) => (
        <button aria-label={ariaLabel} onClick={onClick}>
            {icon}
        </button>
    ),
}));

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div data-testid="dropdown-menu">{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => (
        <div data-testid="dropdown-content">{children}</div>
    ),
    DropdownMenuItem: ({children, onClick}: {children: React.ReactNode; className?: string; onClick?: () => void}) => (
        <button data-testid="dropdown-item" onClick={onClick}>
            {children}
        </button>
    ),
    DropdownMenuSeparator: () => <hr data-testid="dropdown-separator" />,
    DropdownMenuTrigger: ({children}: {asChild?: boolean; children: React.ReactNode}) => (
        <div data-testid="dropdown-trigger">{children}</div>
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
    handleCloseDeleteDialog: hoisted.handleCloseDeleteDialog,
    handleCloseEditDialog: hoisted.handleCloseEditDialog,
    handleShowDeleteDialog: hoisted.handleShowDeleteDialog,
    handleShowEditDialog: hoisted.handleShowEditDialog,
    showDeleteDialog: false,
    showEditDialog: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseDropdownMenu.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderComponent = () => {
    return render(<KnowledgeBaseDropdownMenu knowledgeBase={mockKnowledgeBase} />);
};

describe('KnowledgeBaseDropdownMenu', () => {
    it('renders dropdown menu', () => {
        renderComponent();

        expect(screen.getByTestId('dropdown-menu')).toBeInTheDocument();
    });

    it('renders trigger button with aria label', () => {
        renderComponent();

        expect(screen.getByLabelText('More Knowledge Base Actions')).toBeInTheDocument();
    });

    it('renders Edit menu item', () => {
        renderComponent();

        expect(screen.getByText('Edit')).toBeInTheDocument();
    });

    it('renders Delete menu item', () => {
        renderComponent();

        expect(screen.getByText('Delete')).toBeInTheDocument();
    });

    it('calls handleShowEditDialog when Edit is clicked', async () => {
        renderComponent();

        const editButton = screen.getByText('Edit').closest('button');
        await userEvent.click(editButton!);

        expect(hoisted.handleShowEditDialog).toHaveBeenCalled();
    });

    it('calls handleShowDeleteDialog when Delete is clicked', async () => {
        renderComponent();

        const deleteButton = screen.getByText('Delete').closest('button');
        await userEvent.click(deleteButton!);

        expect(hoisted.handleShowDeleteDialog).toHaveBeenCalled();
    });

    it('shows edit dialog when showEditDialog is true', () => {
        hoisted.mockUseKnowledgeBaseDropdownMenu.mockReturnValue({
            ...defaultMockReturn,
            showEditDialog: true,
        });

        renderComponent();

        expect(screen.getByTestId('edit-dialog')).toBeInTheDocument();
    });

    it('shows delete dialog when showDeleteDialog is true', () => {
        hoisted.mockUseKnowledgeBaseDropdownMenu.mockReturnValue({
            ...defaultMockReturn,
            showDeleteDialog: true,
        });

        renderComponent();

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });
});
