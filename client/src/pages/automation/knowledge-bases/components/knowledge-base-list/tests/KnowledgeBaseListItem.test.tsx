import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import KnowledgeBaseListItem from '../KnowledgeBaseListItem';

const hoisted = vi.hoisted(() => {
    return {
        handleCloseDeleteDialog: vi.fn(),
        handleEditClick: vi.fn(),
        handleEditDialogOpenChange: vi.fn(),
        handleKnowledgeBaseClick: vi.fn(),
        handleShowDeleteDialog: vi.fn(),
        handleTagListClick: vi.fn(),
        mockUseKnowledgeBaseListItem: vi.fn(),
    };
});

vi.mock('../hooks/useKnowledgeBaseListItem', () => ({
    default: hoisted.mockUseKnowledgeBaseListItem,
}));

vi.mock('../KnowledgeBaseListItemDeleteDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="delete-dialog">Delete Dialog</div> : null),
}));

vi.mock('../KnowledgeBaseListItemTagList', () => ({
    default: ({knowledgeBaseId}: {knowledgeBaseId: string}) => (
        <div data-testid={`tag-list-${knowledgeBaseId}`}>Tag List</div>
    ),
}));

vi.mock('@/pages/automation/knowledge-base/components/EditKnowledgeBaseDialog', () => ({
    default: ({open}: {open: boolean}) => (open ? <div data-testid="edit-dialog">Edit Dialog</div> : null),
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
    DropdownMenuSeparator: () => <hr />,
    DropdownMenuTrigger: ({
        children,
        onClick,
    }: {
        asChild?: boolean;
        children: React.ReactNode;
        onClick?: (e: React.MouseEvent) => void;
    }) => (
        <div data-testid="dropdown-trigger" onClick={onClick}>
            {children}
        </div>
    ),
}));

vi.mock('@/components/ui/tooltip', () => ({
    Tooltip: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    TooltipContent: ({children}: {children: React.ReactNode}) => <div data-testid="tooltip-content">{children}</div>,
    TooltipTrigger: ({children}: {children: React.ReactNode}) => <div data-testid="tooltip-trigger">{children}</div>,
}));

const mockKnowledgeBase = {
    description: 'Test description',
    id: 'kb-1',
    lastModifiedDate: '2024-01-15T10:30:00Z',
    maxChunkSize: 1024,
    name: 'Test KB',
    overlap: 200,
};

const mockTags = [
    {id: '1', name: 'Tag 1'},
    {id: '2', name: 'Tag 2'},
];

const mockRemainingTags = [{id: '3', name: 'Tag 3'}];

const defaultMockReturn = {
    handleCloseDeleteDialog: hoisted.handleCloseDeleteDialog,
    handleEditClick: hoisted.handleEditClick,
    handleEditDialogOpenChange: hoisted.handleEditDialogOpenChange,
    handleKnowledgeBaseClick: hoisted.handleKnowledgeBaseClick,
    handleShowDeleteDialog: hoisted.handleShowDeleteDialog,
    handleTagListClick: hoisted.handleTagListClick,
    showDeleteDialog: false,
    showEditDialog: false,
};

beforeEach(() => {
    windowResizeObserver();
    hoisted.mockUseKnowledgeBaseListItem.mockReturnValue({...defaultMockReturn});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('KnowledgeBaseListItem', () => {
    it('renders knowledge base name', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByText('Test KB')).toBeInTheDocument();
    });

    it('renders max chunk size', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByText('Max chunk: 1024')).toBeInTheDocument();
    });

    it('renders overlap', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByText('Overlap: 200')).toBeInTheDocument();
    });

    it('renders tag list', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByTestId('tag-list-kb-1')).toBeInTheDocument();
    });

    it('renders last modified date', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getAllByText(/Modified/)[0]).toBeInTheDocument();
    });

    it('shows Never modified when no lastModifiedDate', () => {
        const kbWithoutDate = {...mockKnowledgeBase, lastModifiedDate: undefined};

        render(
            <KnowledgeBaseListItem knowledgeBase={kbWithoutDate} remainingTags={mockRemainingTags} tags={mockTags} />
        );

        expect(screen.getByText('Never modified')).toBeInTheDocument();
    });

    it('renders dropdown menu', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByTestId('dropdown-menu')).toBeInTheDocument();
    });

    it('calls handleKnowledgeBaseClick when item is clicked', async () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        const itemContainer = screen.getByText('Test KB').closest('[class*="cursor-pointer"]');
        await userEvent.click(itemContainer!);

        expect(hoisted.handleKnowledgeBaseClick).toHaveBeenCalled();
    });

    it('shows edit dialog when showEditDialog is true', () => {
        hoisted.mockUseKnowledgeBaseListItem.mockReturnValue({
            ...defaultMockReturn,
            showEditDialog: true,
        });

        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByTestId('edit-dialog')).toBeInTheDocument();
    });

    it('shows delete dialog when showDeleteDialog is true', () => {
        hoisted.mockUseKnowledgeBaseListItem.mockReturnValue({
            ...defaultMockReturn,
            showDeleteDialog: true,
        });

        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(screen.getByTestId('delete-dialog')).toBeInTheDocument();
    });

    it('passes correct props to hook', () => {
        render(
            <KnowledgeBaseListItem
                knowledgeBase={mockKnowledgeBase}
                remainingTags={mockRemainingTags}
                tags={mockTags}
            />
        );

        expect(hoisted.mockUseKnowledgeBaseListItem).toHaveBeenCalledWith({
            knowledgeBase: mockKnowledgeBase,
        });
    });
});
