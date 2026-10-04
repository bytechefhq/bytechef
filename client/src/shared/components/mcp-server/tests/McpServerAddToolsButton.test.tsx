import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerAddToolsButton, {
    McpServerComponentDialogProps,
    McpServerWorkflowDialogProps,
} from '../McpServerAddToolsButton';

const FakeMcpComponentDialog = ({mcpServerId, onOpenChange}: McpServerComponentDialogProps) => (
    <div role="dialog">
        <span>Component dialog for {mcpServerId}</span>

        <button onClick={() => onOpenChange(false)}>Close component dialog</button>
    </div>
);

const FakeWorkflowDialog = ({mcpServer, onClose}: McpServerWorkflowDialogProps) => (
    <div role="dialog">
        <span>Workflow dialog for {mcpServer.name}</span>

        <button onClick={onClose}>Close workflow dialog</button>
    </div>
);

const renderButton = () =>
    render(
        <McpServerAddToolsButton
            mcpComponentDialog={FakeMcpComponentDialog}
            mcpServer={{id: '1', name: 'mcpserver1'} as McpServer}
            workflowDialog={FakeWorkflowDialog}
        />
    );

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

describe('McpServerAddToolsButton', () => {
    it('opens no dialog initially', () => {
        renderButton();

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('opens the component dialog from the default Add Component button', async () => {
        renderButton();

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();
    });

    it('lists Add Component and Add Workflows in the menu', async () => {
        renderButton();

        await openMenu();

        const menuItems = screen.getAllByRole('menuitem');

        expect(menuItems.map((menuItem) => menuItem.textContent?.trim())).toEqual(['Add Component', 'Add Workflows']);
    });

    it('opens the component dialog from the menu', async () => {
        renderButton();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();
    });

    it('opens the workflow dialog from the menu and closes it again', async () => {
        renderButton();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Add Workflows'}));

        expect(screen.getByText('Workflow dialog for mcpserver1')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close workflow dialog'}));

        expect(screen.queryByText('Workflow dialog for mcpserver1')).not.toBeInTheDocument();
    });

    it('closes the component dialog when it reports closed', async () => {
        renderButton();

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));
        await userEvent.click(screen.getByRole('button', {name: 'Close component dialog'}));

        expect(screen.queryByText('Component dialog for 1')).not.toBeInTheDocument();
    });
});
