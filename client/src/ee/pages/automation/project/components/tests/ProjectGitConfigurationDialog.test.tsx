import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useGetProjectRemoteBranchesQueryMock} = vi.hoisted(() => ({
    useGetProjectRemoteBranchesQueryMock: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/automation/projectGit.queries', () => ({
    useGetProjectRemoteBranchesQuery: useGetProjectRemoteBranchesQueryMock,
}));

import ProjectGitConfigurationDialog from '../ProjectGitConfigurationDialog';

describe('ProjectGitConfigurationDialog', () => {
    beforeEach(() => {
        useGetProjectRemoteBranchesQueryMock.mockReset();
        useGetProjectRemoteBranchesQueryMock.mockReturnValue({data: ['main', 'develop'], isLoading: false});
    });

    it('submits the existing configuration and closes the dialog on success', async () => {
        const onClose = vi.fn();
        const onUpdateProjectGitConfigurationSubmit = vi.fn();

        render(
            <ProjectGitConfigurationDialog
                onClose={onClose}
                onUpdateProjectGitConfigurationSubmit={onUpdateProjectGitConfigurationSubmit}
                projectGitConfiguration={{branch: 'main', enabled: true}}
                projectId={7}
            />
        );

        expect(useGetProjectRemoteBranchesQueryMock).toHaveBeenCalledWith(7, true);
        expect(screen.getByText('Update Git Configuration')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(onUpdateProjectGitConfigurationSubmit).toHaveBeenCalledWith({
                onSuccess: onClose,
                projectGitConfiguration: {branch: 'main', enabled: true},
            })
        );
    });

    it('rejects a missing branch instead of submitting', async () => {
        const onUpdateProjectGitConfigurationSubmit = vi.fn();

        useGetProjectRemoteBranchesQueryMock.mockReturnValue({data: undefined, isLoading: true});

        render(
            <ProjectGitConfigurationDialog
                onClose={vi.fn()}
                onUpdateProjectGitConfigurationSubmit={onUpdateProjectGitConfigurationSubmit}
                projectId={7}
            />
        );

        expect(screen.getByText('Loading branches...')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Branch must be at least 2 characters.')).toBeInTheDocument();
        expect(onUpdateProjectGitConfigurationSubmit).not.toHaveBeenCalled();
    });
});
