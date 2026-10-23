import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectGitConfigurationDialog from '../ProjectGitConfigurationDialog';

const hoisted = vi.hoisted(() => ({
    remoteBranchesQuery: {data: ['main', 'develop'] as string[] | undefined, isLoading: false},
    useGetProjectRemoteBranchesQuery: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.queries', () => ({
    useGetProjectRemoteBranchesQuery: hoisted.useGetProjectRemoteBranchesQuery,
}));

describe('ProjectGitConfigurationDialog', () => {
    const onClose = vi.fn();
    const onUpdateProjectGitConfigurationSubmit = vi.fn();

    beforeEach(() => {
        hoisted.remoteBranchesQuery = {data: ['main', 'develop'], isLoading: false};
        hoisted.useGetProjectRemoteBranchesQuery.mockReset();
        hoisted.useGetProjectRemoteBranchesQuery.mockImplementation(() => hoisted.remoteBranchesQuery);

        onClose.mockReset();
        onUpdateProjectGitConfigurationSubmit.mockReset();
    });

    const renderDialog = (projectGitConfiguration?: {branch?: string; enabled?: boolean}) =>
        render(
            <ProjectGitConfigurationDialog
                onClose={onClose}
                onUpdateProjectGitConfigurationSubmit={onUpdateProjectGitConfigurationSubmit}
                projectGitConfiguration={projectGitConfiguration}
                projectId={7}
            />
        );

    it('loads the remote branches of the project', () => {
        renderDialog();

        expect(screen.getByText('Update Git Configuration')).toBeInTheDocument();
        expect(screen.getByText('Select a branch')).toBeInTheDocument();
        expect(hoisted.useGetProjectRemoteBranchesQuery).toHaveBeenCalledWith(7, true);
    });

    it('disables the branch select while the branches load', () => {
        hoisted.remoteBranchesQuery = {data: undefined, isLoading: true};

        renderDialog();

        expect(screen.getByText('Loading branches...')).toBeInTheDocument();
        expect(screen.getByRole('combobox')).toBeDisabled();
    });

    it('submits the configuration and closes on success', async () => {
        renderDialog({branch: 'main', enabled: false});

        fireEvent.click(screen.getByRole('switch'));
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(onUpdateProjectGitConfigurationSubmit).toHaveBeenCalledWith({
                onSuccess: onClose,
                projectGitConfiguration: {branch: 'main', enabled: true},
            })
        );
    });

    it('requires a branch before submitting', async () => {
        renderDialog();

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Branch must be at least 2 characters.')).toBeInTheDocument();
        expect(onUpdateProjectGitConfigurationSubmit).not.toHaveBeenCalled();
    });
});
