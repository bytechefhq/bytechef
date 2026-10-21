import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectDeploymentsButton from '@/pages/automation/project/components/project-header/components/ProjectDeploymentsButton';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

it('calls onClick when the project deployments button is clicked', () => {
    const onClick = vi.fn();

    render(
        <TooltipProvider>
            <ProjectDeploymentsButton onClick={onClick} />
        </TooltipProvider>
    );

    fireEvent.click(screen.getByLabelText('Project Deployments'));

    expect(onClick).toHaveBeenCalledTimes(1);
});
