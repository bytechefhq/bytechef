import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApprovalTaskSortMenu from '../ApprovalTaskSortMenu';

describe('ApprovalTaskSortMenu', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('renders with the design-system DropdownMenu', async () => {
        render(<ApprovalTaskSortMenu onSortChange={vi.fn()} sortBy="created" sortDirection="asc" />);

        await userEvent.click(screen.getByRole('button', {name: 'Sort options'}));

        expect(await screen.findByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: /Title/})).toHaveClass('text-content-neutral-primary');
    });

    it('shows the direction arrow only on the active sort option', async () => {
        render(<ApprovalTaskSortMenu onSortChange={vi.fn()} sortBy="title" sortDirection="desc" />);

        await userEvent.click(screen.getByRole('button', {name: 'Sort options'}));

        const activeItem = await screen.findByRole('menuitem', {name: /Title/});

        expect(within(activeItem).getByText('↓')).toBeInTheDocument();
        expect(within(screen.getByRole('menuitem', {name: 'Created Date'})).queryByText(/[↑↓]/)).toBeNull();
    });

    it('sorts ascending by an option that is not active yet', async () => {
        const onSortChange = vi.fn();

        render(<ApprovalTaskSortMenu onSortChange={onSortChange} sortBy="created" sortDirection="desc" />);

        await userEvent.click(screen.getByRole('button', {name: 'Sort options'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Priority'}));

        expect(onSortChange).toHaveBeenCalledWith('priority', 'asc');
    });

    it('flips the direction of the option that is already active', async () => {
        const onSortChange = vi.fn();

        render(<ApprovalTaskSortMenu onSortChange={onSortChange} sortBy="title" sortDirection="asc" />);

        await userEvent.click(screen.getByRole('button', {name: 'Sort options'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: /Title/}));

        expect(onSortChange).toHaveBeenCalledWith('title', 'desc');
    });
});
