import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceListItem from './WorkspaceListItem';

vi.mock('@/ee/shared/mutations/automation/workspaces.mutations', () => ({
    useCreateWorkspaceMutation: () => ({mutate: vi.fn()}),
    useDeleteWorkspaceMutation: () => ({mutate: vi.fn()}),
    useUpdateWorkspaceMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn((selector: (state: {account: {id: number}}) => unknown) =>
        selector({account: {id: 1}})
    ),
}));

const onOpen = vi.fn();

const renderWorkspaceListItem = (isCurrentWorkspace = false) =>
    render(
        <ul>
            <WorkspaceListItem
                isCurrentWorkspace={isCurrentWorkspace}
                onOpen={onOpen}
                workspace={{id: 5, name: 'Workspace2'}}
            />
        </ul>
    );

describe('WorkspaceListItem', () => {
    beforeEach(() => {
        onOpen.mockReset();
    });

    it('marks the current workspace', () => {
        renderWorkspaceListItem(true);

        expect(screen.getByText('Current')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Workspace2'})).toHaveAttribute('aria-current', 'true');
    });

    it('does not mark a workspace that is not current', () => {
        renderWorkspaceListItem();

        expect(screen.queryByText('Current')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Workspace2'})).not.toHaveAttribute('aria-current');
    });

    it('opens the workspace when it is clicked', async () => {
        const user = userEvent.setup();

        renderWorkspaceListItem();

        await user.click(screen.getByRole('button', {name: 'Workspace2'}));

        expect(onOpen).toHaveBeenCalledWith(5);
    });

    it('opens the workspace from the keyboard', async () => {
        const user = userEvent.setup();

        renderWorkspaceListItem();

        await user.tab();

        expect(screen.getByRole('button', {name: 'Workspace2'})).toHaveFocus();

        await user.keyboard('{Enter}');

        expect(onOpen).toHaveBeenCalledWith(5);
    });

    it('does not open the workspace when the actions menu is used', async () => {
        const user = userEvent.setup();

        renderWorkspaceListItem();

        await user.click(screen.getByRole('button', {name: 'Workspace actions'}));
        await user.click(screen.getByRole('menuitem', {name: 'Edit'}));

        expect(screen.getByText('Edit Workspace')).toBeInTheDocument();
        expect(onOpen).not.toHaveBeenCalled();
    });
});
