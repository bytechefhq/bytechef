import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerToolsAddButton from './McpServerToolsAddButton';

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog', () => ({
    default: ({mcpServerId, onOpenChange}: {mcpServerId: string; onOpenChange: (open: boolean) => void}) => (
        <div role="dialog">
            <span>Component dialog for {mcpServerId}</span>

            <button onClick={() => onOpenChange(false)}>Close component dialog</button>
        </div>
    ),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog', () => ({
    default: ({mcpServer, onClose}: {mcpServer: McpServer; onClose: () => void}) => (
        <div role="dialog">
            <span>Workflow dialog for {mcpServer.name}</span>

            <button onClick={onClose}>Close workflow dialog</button>
        </div>
    ),
}));

const mcpServer = {id: '1', name: 'mcpserver1'} as McpServer;

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Add Tools'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerToolsAddButton', () => {
    it('opens no dialog initially', () => {
        render(<McpServerToolsAddButton mcpServer={mcpServer} />);

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('opens the component dialog from the default Add Component button', async () => {
        render(<McpServerToolsAddButton mcpServer={mcpServer} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();
    });

    it('lists Add Component and Add Workflows in the menu', async () => {
        render(<McpServerToolsAddButton mcpServer={mcpServer} />);

        await openMenu();

        const menuItems = screen.getAllByRole('menuitem');

        expect(menuItems.map((menuItem) => menuItem.textContent?.trim())).toEqual(['Add Component', 'Add Workflows']);
    });

    it('opens the component dialog from the menu', async () => {
        render(<McpServerToolsAddButton mcpServer={mcpServer} />);

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();
    });

    it('opens the workflow dialog from the menu and closes it again', async () => {
        render(<McpServerToolsAddButton mcpServer={mcpServer} />);

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Add Workflows'}));

        expect(screen.getByText('Workflow dialog for mcpserver1')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close workflow dialog'}));

        expect(screen.queryByText('Workflow dialog for mcpserver1')).not.toBeInTheDocument();
    });

    it('closes the component dialog when it reports closed', async () => {
        render(<McpServerToolsAddButton mcpServer={mcpServer} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));
        await userEvent.click(screen.getByRole('button', {name: 'Close component dialog'}));

        expect(screen.queryByText('Component dialog for 1')).not.toBeInTheDocument();
    });
});
