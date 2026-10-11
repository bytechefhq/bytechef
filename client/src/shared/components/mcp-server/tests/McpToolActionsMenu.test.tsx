import {Popover} from '@/components/ui/popover';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import McpToolActionsMenu from '../McpToolActionsMenu';

describe('McpToolActionsMenu', () => {
    it('calls the handler of each action', async () => {
        const user = userEvent.setup();
        const handleConfigureClick = vi.fn();
        const handleDeleteClick = vi.fn();
        const handleEditClick = vi.fn();

        render(
            <McpToolActionsMenu
                label="Lead intake"
                onConfigureClick={handleConfigureClick}
                onDeleteClick={handleDeleteClick}
                onEditClick={handleEditClick}
            />
        );

        for (const [actionName, handler] of [
            ['Configure', handleConfigureClick],
            ['Edit', handleEditClick],
            ['Delete', handleDeleteClick],
        ] as const) {
            await user.click(screen.getByRole('button', {name: 'Lead intake actions'}));
            await user.click(await screen.findByRole('menuitem', {name: actionName}));

            expect(handler).toHaveBeenCalledTimes(1);
        }
    });

    it('leaves out Edit without an edit handler', async () => {
        const user = userEvent.setup();

        render(
            <Popover>
                <McpToolActionsMenu
                    anchorsPopover
                    label="Create Opportunity"
                    onConfigureClick={vi.fn()}
                    onDeleteClick={vi.fn()}
                />
            </Popover>
        );

        await user.click(screen.getByRole('button', {name: 'Create Opportunity actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Configure'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
    });
});
