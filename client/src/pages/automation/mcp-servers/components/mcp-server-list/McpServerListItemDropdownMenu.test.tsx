import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerListItemDropdownMenu from './McpServerListItemDropdownMenu';

const hoistedScope = vi.hoisted(() => ({grantedScopes: ['MCP_DELETE', 'MCP_EDIT'] as string[]}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) =>
        hoistedScope.grantedScopes.includes(scope),
}));

const defaultProps = {
    mcpServer: {id: '1', name: 'mcpserver1'} as McpServer,
    onAddComponentClick: vi.fn(),
    onAddWorkflowsClick: vi.fn(),
    onDeleteClick: vi.fn(),
    onEditClick: vi.fn(),
};

beforeEach(() => {
    hoistedScope.grantedScopes = ['MCP_DELETE', 'MCP_EDIT'];

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('McpServerListItemDropdownMenu', () => {
    it('shows every item to a member with MCP_EDIT and MCP_DELETE', async () => {
        render(<McpServerListItemDropdownMenu {...defaultProps} />);

        await userEvent.setup().click(screen.getByRole('button', {name: 'MCP Server Actions'}));

        expect(screen.getByRole('menuitem', {name: 'Add Component'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Add Workflows'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('hides the edit items from a member without MCP_EDIT', async () => {
        hoistedScope.grantedScopes = ['MCP_DELETE'];

        render(<McpServerListItemDropdownMenu {...defaultProps} />);

        await userEvent.setup().click(screen.getByRole('button', {name: 'MCP Server Actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Add Component'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Add Workflows'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
    });

    it('renders no menu for a member without MCP_EDIT or MCP_DELETE', () => {
        hoistedScope.grantedScopes = [];

        render(<McpServerListItemDropdownMenu {...defaultProps} />);

        expect(screen.queryByRole('button', {name: 'MCP Server Actions'})).not.toBeInTheDocument();
    });
});
