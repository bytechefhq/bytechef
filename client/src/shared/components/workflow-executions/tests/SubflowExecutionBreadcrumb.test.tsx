import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import SubflowExecutionBreadcrumb from '../SubflowExecutionBreadcrumb';

const onNavigateMiddleFirst = vi.fn();
const onNavigateMiddleSecond = vi.fn();

const items = [
    {label: 'Root workflow', onNavigate: vi.fn()},
    {label: 'First subflow', onNavigate: onNavigateMiddleFirst},
    {label: 'Second subflow', onNavigate: onNavigateMiddleSecond},
    {label: 'Current subflow'},
];

describe('SubflowExecutionBreadcrumb', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('renders the hidden middle levels with the design-system DropdownMenu', async () => {
        render(<SubflowExecutionBreadcrumb items={items} onBackClick={vi.fn()} />);

        await userEvent.click(screen.getByText('More'));

        expect(await screen.findByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'First subflow'})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Second subflow'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Root workflow'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Current subflow'})).not.toBeInTheDocument();
    });

    it('navigates to the level that is picked from the menu', async () => {
        render(<SubflowExecutionBreadcrumb items={items} onBackClick={vi.fn()} />);

        await userEvent.click(screen.getByText('More'));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Second subflow'}));

        expect(onNavigateMiddleSecond).toHaveBeenCalledTimes(1);
        expect(onNavigateMiddleFirst).not.toHaveBeenCalled();
    });
});
