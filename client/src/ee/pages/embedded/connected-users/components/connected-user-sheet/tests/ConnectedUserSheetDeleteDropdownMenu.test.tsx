import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
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

    it('should not offer Change configuration unless the handler is given', async () => {
        render(<ConnectedUserSheetDeleteDropdownMenu onDeleteClick={hoisted.onDeleteClick} />);

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Change configuration'})).not.toBeInTheDocument();
    });

    it('should list Change configuration before Delete and call its handler', async () => {
        const onChangeConfigurationClick = vi.fn();

        render(
            <TooltipProvider>
                <ConnectedUserSheetDeleteDropdownMenu
                    onChangeConfigurationClick={onChangeConfigurationClick}
                    onDeleteClick={hoisted.onDeleteClick}
                />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual([
            'Change configuration…',
            'Delete',
        ]);

        await userEvent.click(screen.getByRole('menuitem', {name: 'Change configuration'}));

        expect(onChangeConfigurationClick).toHaveBeenCalledTimes(1);
    });

    it('should disable Change configuration with the given reason', async () => {
        render(
            <TooltipProvider>
                <ConnectedUserSheetDeleteDropdownMenu
                    changeConfigurationDisabledReason="No other configuration in this environment"
                    onChangeConfigurationClick={vi.fn()}
                    onDeleteClick={hoisted.onDeleteClick}
                />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        const item = screen.getByRole('menuitem', {name: 'Change configuration'});

        expect(item).toHaveAttribute('aria-disabled', 'true');

        await userEvent.hover(item.parentElement as HTMLElement);

        await waitFor(async () =>
            expect((await screen.findAllByText('No other configuration in this environment')).length).toBeGreaterThan(0)
        );
    });
});
