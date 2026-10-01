import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {render, screen, within} from '@/shared/util/test-utils';
import {act} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceList from './WorkspaceList';

const hoisted = vi.hoisted(() => ({
    openWorkspaceProjects: vi.fn(),
}));

vi.mock('@/ee/pages/settings/automation/workspaces/hooks/useOpenWorkspaceProjects', () => ({
    default: () => hoisted.openWorkspaceProjects,
}));

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

const workspaces = [
    {id: 1, name: 'Default'},
    {id: 5, name: 'Workspace2'},
];

const getWorkspaceRow = (name: string) => screen.getByRole('button', {name}).closest('li')!;

describe('WorkspaceList', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 1});

        hoisted.openWorkspaceProjects.mockReset();
        hoisted.openWorkspaceProjects.mockImplementation((workspaceId: number) =>
            useWorkspaceStore.setState({currentWorkspaceId: workspaceId})
        );
    });

    it('marks the current workspace', () => {
        render(<WorkspaceList workspaces={workspaces} />);

        expect(within(getWorkspaceRow('Default')).getByText('Current')).toBeInTheDocument();
        expect(within(getWorkspaceRow('Workspace2')).queryByText('Current')).not.toBeInTheDocument();
    });

    it('follows a workspace switch made elsewhere', () => {
        render(<WorkspaceList workspaces={workspaces} />);

        act(() => {
            useWorkspaceStore.setState({currentWorkspaceId: 5});
        });

        expect(within(getWorkspaceRow('Workspace2')).getByText('Current')).toBeInTheDocument();
        expect(within(getWorkspaceRow('Default')).queryByText('Current')).not.toBeInTheDocument();
    });

    it('keeps the marker in place while navigating away to the opened workspace', async () => {
        const user = userEvent.setup();

        render(<WorkspaceList workspaces={workspaces} />);

        await user.click(screen.getByRole('button', {name: 'Workspace2'}));

        expect(hoisted.openWorkspaceProjects).toHaveBeenCalledWith(5);
        expect(useWorkspaceStore.getState().currentWorkspaceId).toBe(5);
        expect(within(getWorkspaceRow('Default')).getByText('Current')).toBeInTheDocument();
        expect(within(getWorkspaceRow('Workspace2')).queryByText('Current')).not.toBeInTheDocument();
    });
});
