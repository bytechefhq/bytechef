import {Dialog, DialogContent, DialogTitle, DialogTrigger} from '@/components/Dialog';
import {TooltipProvider} from '@/components/ui/tooltip';
import DeployButton from '@/pages/automation/project/components/project-header/components/DeployButton';
import {Project} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({onClose, triggerNode}: {onClose?: () => void; triggerNode?: ReactNode}) =>
        triggerNode ? (
            <Dialog>
                <DialogTrigger asChild>{triggerNode}</DialogTrigger>

                <DialogContent>
                    <DialogTitle>Deploy Project</DialogTitle>
                </DialogContent>
            </Dialog>
        ) : (
            <div>
                <span>Controlled Deploy Dialog</span>

                <button onClick={onClose}>Close controlled</button>
            </div>
        ),
}));

const renderDeployButton = (
    project: Project,
    controlled: {onOpenChange?: (open: boolean) => void; open?: boolean} = {}
) =>
    render(
        <TooltipProvider>
            <DeployButton onOpenChange={controlled.onOpenChange} open={controlled.open} project={project} />
        </TooltipProvider>
    );

const publishedProject = {id: 7, lastProjectVersion: 2, lastPublishedDate: new Date(), name: 'Orders'} as Project;

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

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Publish the project before deploying it.');
    });

    it('renders the deployment dialog without a click when open is true', async () => {
        renderDeployButton(publishedProject, {open: true});

        expect(await screen.findByText('Controlled Deploy Dialog')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
    });

    it('does not render the controlled deployment dialog when open is false', () => {
        renderDeployButton(publishedProject, {open: false});

        expect(screen.queryByText('Controlled Deploy Dialog')).not.toBeInTheDocument();
    });

    it('calls onOpenChange with false when the controlled deployment dialog closes', async () => {
        const onOpenChange = vi.fn();

        renderDeployButton(publishedProject, {onOpenChange, open: true});

        await userEvent.click(await screen.findByRole('button', {name: 'Close controlled'}));

        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('clears the open flag and renders no dialog for an unpublished project', () => {
        const onOpenChange = vi.fn();

        renderDeployButton({id: 7, name: 'Orders'} as Project, {onOpenChange, open: true});

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(screen.queryByText('Controlled Deploy Dialog')).not.toBeInTheDocument();
    });
});
