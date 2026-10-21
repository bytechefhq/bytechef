import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheetDeleteDropdownMenu from '../ConnectedUserSheetDeleteDropdownMenu';

const hoisted = vi.hoisted(() => ({
    onDeleteClick: vi.fn(),
}));

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ConnectedUserSheetDeleteDropdownMenu', () => {
    it('should render the menu trigger button', () => {
        render(<ConnectedUserSheetDeleteDropdownMenu onDeleteClick={hoisted.onDeleteClick} />);

        expect(screen.getByRole('button', {name: 'More actions'})).toBeInTheDocument();
    });

    it('should render with the design-system DropdownMenu', async () => {
        render(<ConnectedUserSheetDeleteDropdownMenu onDeleteClick={hoisted.onDeleteClick} />);

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should call onDeleteClick when clicking Delete', async () => {
        render(<ConnectedUserSheetDeleteDropdownMenu onDeleteClick={hoisted.onDeleteClick} />);

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(hoisted.onDeleteClick).toHaveBeenCalledTimes(1);
    });
});
