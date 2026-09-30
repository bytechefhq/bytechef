import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';
import {render, screen} from '@/shared/util/test-utils';
import {Mock, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialogBasicStepProjectVersionsSelect from './ProjectDeploymentDialogBasicStepProjectVersionsSelect';

vi.mock('@/shared/queries/automation/projectVersions.queries', () => ({
    useGetProjectVersionsQuery: vi.fn(),
}));

const noPublishedVersionMessage = 'This project has no published version yet. Publish it first.';

describe('ProjectDeploymentDialogBasicStepProjectVersionsSelect', () => {
    it('should explain that the project has no published version', () => {
        (useGetProjectVersionsQuery as Mock).mockReturnValue({
            data: [{status: ProjectStatus.Draft, version: 1}],
            isPending: false,
        });

        render(<ProjectDeploymentDialogBasicStepProjectVersionsSelect onChange={vi.fn()} projectId={1} />);

        expect(screen.getByText(noPublishedVersionMessage)).toBeInTheDocument();
        expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
    });

    it('should show the version select when the project has a published version', () => {
        (useGetProjectVersionsQuery as Mock).mockReturnValue({
            data: [
                {status: ProjectStatus.Published, version: 1},
                {status: ProjectStatus.Draft, version: 2},
            ],
            isPending: false,
        });

        render(<ProjectDeploymentDialogBasicStepProjectVersionsSelect onChange={vi.fn()} projectId={1} />);

        expect(screen.getByRole('combobox')).toBeInTheDocument();
        expect(screen.queryByText(noPublishedVersionMessage)).not.toBeInTheDocument();
    });
});
