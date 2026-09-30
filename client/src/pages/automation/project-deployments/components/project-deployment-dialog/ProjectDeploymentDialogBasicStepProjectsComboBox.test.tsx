import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import ProjectDeploymentDialogBasicStepProjectsComboBox from './ProjectDeploymentDialogBasicStepProjectsComboBox';

describe('ProjectDeploymentDialogBasicStepProjectsComboBox', () => {
    it('should tell the user to add a workflow and publish when no project can be deployed', async () => {
        render(<ProjectDeploymentDialogBasicStepProjectsComboBox onBlur={vi.fn()} onChange={vi.fn()} projects={[]} />);

        await userEvent.click(screen.getByRole('combobox'));

        expect(
            await screen.findByText('No projects with workflows yet. Add a workflow to a project and publish it.')
        ).toBeInTheDocument();
    });
});
