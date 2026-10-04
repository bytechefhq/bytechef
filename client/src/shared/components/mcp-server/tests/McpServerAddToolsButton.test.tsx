import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerAddToolsButton from '../McpServerAddToolsButton';

const renderButton = () =>
    render(
        <McpServerAddToolsButton
            renderMcpComponentDialog={(onClose) => (
                <div role="dialog">
                    <span>Component dialog</span>

                    <button onClick={onClose}>Close component dialog</button>
                </div>
            )}
            renderWorkflowDialog={(onClose) => (
                <div role="dialog">
                    <span>Workflow dialog</span>

                    <button onClick={onClose}>Close workflow dialog</button>
                </div>
            )}
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

        expect(screen.getByText('Component dialog')).toBeInTheDocument();
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

        expect(screen.getByText('Component dialog')).toBeInTheDocument();
    });

    it('opens the workflow dialog from the menu and closes it again', async () => {
        renderButton();

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Add Workflows'}));

        expect(screen.getByText('Workflow dialog')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Close workflow dialog'}));

        expect(screen.queryByText('Workflow dialog')).not.toBeInTheDocument();
    });

    it('closes the component dialog when it reports closed', async () => {
        renderButton();

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));
        await userEvent.click(screen.getByRole('button', {name: 'Close component dialog'}));

        expect(screen.queryByText('Component dialog')).not.toBeInTheDocument();
    });
});
