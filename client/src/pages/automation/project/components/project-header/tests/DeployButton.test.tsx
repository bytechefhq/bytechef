import {Dialog, DialogContent, DialogTitle, DialogTrigger} from '@/components/Dialog';
import {TooltipProvider} from '@/components/ui/tooltip';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import {Project} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({triggerNode}: {triggerNode: ReactNode}) => (
        <Dialog>
            <DialogTrigger asChild>{triggerNode}</DialogTrigger>

            <DialogContent>
                <DialogTitle>Deploy Project</DialogTitle>
            </DialogContent>
        </Dialog>
    ),
}));

const renderDeployButton = (project: Project) =>
    render(
        <TooltipProvider>
            <DeployButton project={project} />
        </TooltipProvider>
    );

describe('DeployButton', () => {
    it('shows a tooltip on hover and opens the deployment dialog on click', async () => {
        renderDeployButton({id: 7, lastProjectVersion: 2, lastPublishedDate: new Date(), name: 'Orders'} as Project);

        const deployButton = screen.getByRole('button', {name: 'Deploy'});

        await userEvent.hover(deployButton);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Deploy the published project version');

        await userEvent.click(deployButton);

        expect(await screen.findByText('Deploy Project')).toBeInTheDocument();
    });

    it('explains why deploying is disabled for an unpublished project', async () => {
        renderDeployButton({id: 7, name: 'Orders'} as Project);

        const deployButton = screen.getByRole('button', {name: 'Deploy'});

        expect(deployButton).toBeDisabled();

        await userEvent.hover(deployButton.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Publish the project to enable deployment');
    });
});
