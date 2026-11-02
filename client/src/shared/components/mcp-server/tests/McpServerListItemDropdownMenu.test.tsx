import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerListItemDropdownMenu from '../McpServerListItemDropdownMenu';

const mcpServer = {id: '1', name: 'mcpserver1'} as McpServer;

const defaultProps = {
    mcpServer,
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerListItemDropdownMenu', () => {
    it('should render with the design-system DropdownMenu', async () => {
        render(<McpServerListItemDropdownMenu mcpServer={mcpServer} onDeleteClick={vi.fn()} onEditClick={vi.fn()} />);

        await userEvent.click(screen.getByRole('button'));

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('calls onEditClick when Edit is clicked', async () => {
        const onEditClick = vi.fn();

        render(
            <McpServerListItemDropdownMenu mcpServer={mcpServer} onDeleteClick={vi.fn()} onEditClick={onEditClick} />
        );

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(onEditClick).toHaveBeenCalledTimes(1);
    });

    it('calls onDeleteClick when Delete is clicked', async () => {
        const onDeleteClick = vi.fn();

        render(
            <McpServerListItemDropdownMenu mcpServer={mcpServer} onDeleteClick={onDeleteClick} onEditClick={vi.fn()} />
        );

        await userEvent.click(screen.getByRole('button'));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(onDeleteClick).toHaveBeenCalledTimes(1);
    });

    it('shows every item when the caller may edit and delete', async () => {
        render(<McpServerListItemDropdownMenu {...defaultProps} canDelete canEdit />);

        await userEvent.setup().click(screen.getByRole('button', {name: 'MCP Server Actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('shows every item when no permission is passed, as on the embedded page', async () => {
        render(<McpServerListItemDropdownMenu {...defaultProps} />);

        await userEvent.setup().click(screen.getByRole('button', {name: 'MCP Server Actions'}));

        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('hides Edit from a caller without edit permission', async () => {
        render(<McpServerListItemDropdownMenu {...defaultProps} canDelete canEdit={false} />);

        await userEvent.setup().click(screen.getByRole('button', {name: 'MCP Server Actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('renders no menu for a caller without edit or delete permission', () => {
        render(<McpServerListItemDropdownMenu {...defaultProps} canDelete={false} canEdit={false} />);

        expect(screen.queryByRole('button', {name: 'MCP Server Actions'})).not.toBeInTheDocument();
    });
});
