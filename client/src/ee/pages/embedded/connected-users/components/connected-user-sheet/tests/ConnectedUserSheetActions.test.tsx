import {ConnectedUser} from '@/ee/shared/middleware/embedded/connected-user';
import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserSheetActions from '../ConnectedUserSheetActions';

const hoisted = vi.hoisted(() => ({
    deleteConnectedUserMutate: vi.fn(),
    enableConnectedUserMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/connectedUsers.mutations', () => ({
    useDeleteConnectedUserMutation: () => ({isPending: false, mutate: hoisted.deleteConnectedUserMutate}),
    useEnableConnectedUserMutation: () => ({mutate: hoisted.enableConnectedUserMutate}),
}));

const enabledConnectedUser: ConnectedUser = {enabled: true, externalId: 'external-1', id: 11, name: 'Ada Lovelace'};

const disabledConnectedUser: ConnectedUser = {enabled: false, externalId: 'external-2', id: 22, name: 'Grace Hopper'};

const openMenu = async () => {
    // The ellipsis trigger has no accessible name and is the only button rendered (the delete dialog starts closed)
    await userEvent.click(screen.getByRole('button'));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ConnectedUserSheetActions', () => {
    it('should render with the design-system DropdownMenu', async () => {
        render(<ConnectedUserSheetActions connectedUser={enabledConnectedUser} />);

        await openMenu();

        expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Disable'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toHaveClass('text-content-destructive-primary');
    });

    it('should disable an enabled connected user when clicking Disable', async () => {
        render(<ConnectedUserSheetActions connectedUser={enabledConnectedUser} />);

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Enable'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Disable'}));

        expect(hoisted.enableConnectedUserMutate).toHaveBeenCalledWith({enable: false, id: 11});
    });

    it('should enable a disabled connected user when clicking Enable', async () => {
        render(<ConnectedUserSheetActions connectedUser={disabledConnectedUser} />);

        await openMenu();

        expect(screen.queryByRole('menuitem', {name: 'Disable'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Enable'}));

        expect(hoisted.enableConnectedUserMutate).toHaveBeenCalledWith({enable: true, id: 22});
    });

    it('should open the delete dialog and delete the connected user when clicking Delete', async () => {
        render(<ConnectedUserSheetActions connectedUser={enabledConnectedUser} />);

        await openMenu();

        await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));

        const deleteDialog = await screen.findByRole('alertdialog');

        await userEvent.click(within(deleteDialog).getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteConnectedUserMutate).toHaveBeenCalledWith({id: 11});
    });
});
