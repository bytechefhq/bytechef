import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import ProjectListItemDeployButton from './ProjectListItemDeployButton';

const renderDeployButton = (hasWorkflows: boolean, onClick = vi.fn()) => {
    render(
        <TooltipProvider>
            <ProjectListItemDeployButton hasWorkflows={hasWorkflows} onClick={onClick} />
        </TooltipProvider>
    );

    return onClick;
};

describe('ProjectListItemDeployButton', () => {
    it('should disable deploying and explain why when the project has no workflows', async () => {
        const onClick = renderDeployButton(false);

        const deployButton = screen.getByRole('button', {name: 'Deploy'});

        expect(deployButton).toBeDisabled();

        await userEvent.hover(deployButton.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Add a workflow before deploying the project.');

        await userEvent.click(deployButton, {pointerEventsCheck: 0});

        expect(onClick).not.toHaveBeenCalled();
    });

    it('should deploy when the project has workflows', async () => {
        const onClick = renderDeployButton(true);

        const deployButton = screen.getByRole('button', {name: 'Deploy'});

        expect(deployButton).toBeEnabled();

        await userEvent.click(deployButton);

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
