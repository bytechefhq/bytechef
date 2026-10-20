import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import McpToolActionsMenu from '../McpToolActionsMenu';

describe('McpToolActionsMenu', () => {
    it('should render with the design-system DropdownMenu', async () => {
        const user = userEvent.setup();

        render(<McpToolActionsMenu label="Lead intake" onDeleteClick={vi.fn()} onEditClick={vi.fn()} />);

        await user.click(screen.getByRole('button', {name: 'Lead intake actions'}));

        expect(await screen.findByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('calls the handler of each action', async () => {
        const user = userEvent.setup();
        const handleDeleteClick = vi.fn();
        const handleEditClick = vi.fn();

        render(
            <McpToolActionsMenu label="Lead intake" onDeleteClick={handleDeleteClick} onEditClick={handleEditClick} />
        );

        for (const [actionName, handler] of [
            ['Edit', handleEditClick],
            ['Delete', handleDeleteClick],
        ] as const) {
            await user.click(screen.getByRole('button', {name: 'Lead intake actions'}));
            await user.click(await screen.findByRole('menuitem', {name: actionName}));

            expect(handler).toHaveBeenCalledTimes(1);
        }
    });

    it('leaves out Edit without an edit handler and never offers Configure', async () => {
        const user = userEvent.setup();

        render(<McpToolActionsMenu label="Create Opportunity" onDeleteClick={vi.fn()} />);

        await user.click(screen.getByRole('button', {name: 'Create Opportunity actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Edit'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Configure'})).not.toBeInTheDocument();
    });
});
