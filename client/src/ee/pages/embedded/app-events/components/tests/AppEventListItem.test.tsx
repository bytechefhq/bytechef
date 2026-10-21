import {AppEvent} from '@/ee/shared/middleware/embedded/configuration';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AppEventListItem from '../AppEventListItem';

const hoisted = vi.hoisted(() => ({
    deleteAppEventMutate: vi.fn(),
}));

vi.mock('@/ee/pages/embedded/app-events/components/AppEventDialog', () => ({
    default: ({onClose}: {onClose: () => void}) => (
        <div data-testid="app-event-dialog">
            <button onClick={onClose} type="button">
                Close app event dialog
            </button>
        </div>
    ),
}));

vi.mock('@/ee/shared/mutations/embedded/appEvents.mutations', () => ({
    useDeleteAppEventMutation: () => ({mutate: hoisted.deleteAppEventMutate}),
}));

const appEvent: AppEvent = {id: 5, name: 'User signed up', schema: '{}'};

const renderAppEventListItem = () =>
    render(
        <ul>
            <AppEventListItem appEvent={appEvent} />
        </ul>
    );

const openMenu = async () => {
    // The ellipsis trigger has no accessible name and is the only button rendered (no created date, dialogs closed)
    await userEvent.click(screen.getByRole('button'));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AppEventListItem', () => {
    it('should render with the design-system DropdownMenu', async () => {
        renderAppEventListItem();

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should open the edit dialog when clicking Edit', async () => {
        renderAppEventListItem();

        expect(screen.queryByTestId('app-event-dialog')).not.toBeInTheDocument();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByTestId('app-event-dialog')).toBeInTheDocument();
    });

    it('should open the delete dialog and delete the app event when clicking Delete', async () => {
        renderAppEventListItem();

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteAppEventMutate).toHaveBeenCalledWith(5);

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    });
});
