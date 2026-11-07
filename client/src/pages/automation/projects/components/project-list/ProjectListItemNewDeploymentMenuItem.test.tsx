import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/DropdownMenu/DropdownMenu';
import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import ProjectListItemNewDeploymentMenuItem from './ProjectListItemNewDeploymentMenuItem';

const renderNewDeploymentMenuItem = (hasWorkflows: boolean, isPublished: boolean, onClick = vi.fn()) => {
    render(
        <TooltipProvider>
            <DropdownMenu open>
                <DropdownMenuTrigger>More Project Actions</DropdownMenuTrigger>

                <DropdownMenuContent>
                    <ProjectListItemNewDeploymentMenuItem
                        hasWorkflows={hasWorkflows}
                        isPublished={isPublished}
                        onClick={onClick}
                    />
                </DropdownMenuContent>
            </DropdownMenu>
        </TooltipProvider>
    );

    return onClick;
};

describe('ProjectListItemNewDeploymentMenuItem', () => {
    it('should disable deploying and explain why when the project has no workflows', async () => {
        const onClick = renderNewDeploymentMenuItem(false, true);

        const newDeploymentMenuItem = screen.getByRole('menuitem', {name: 'New Deployment'});

        expect(newDeploymentMenuItem).toHaveAttribute('data-disabled');

        await userEvent.hover(newDeploymentMenuItem.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Add a workflow before deploying the project.');

        await userEvent.click(newDeploymentMenuItem, {pointerEventsCheck: 0});

        expect(onClick).not.toHaveBeenCalled();
    });

    it('should disable deploying and explain why when the project is not published', async () => {
        const onClick = renderNewDeploymentMenuItem(true, false);

        const newDeploymentMenuItem = screen.getByRole('menuitem', {name: 'New Deployment'});

        expect(newDeploymentMenuItem).toHaveAttribute('data-disabled');

        await userEvent.hover(newDeploymentMenuItem.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Publish the project before deploying it.');

        await userEvent.click(newDeploymentMenuItem, {pointerEventsCheck: 0});

        expect(onClick).not.toHaveBeenCalled();
    });

    it('should deploy when the project is published and has workflows', async () => {
        const onClick = renderNewDeploymentMenuItem(true, true);

        const newDeploymentMenuItem = screen.getByRole('menuitem', {name: 'New Deployment'});

        expect(newDeploymentMenuItem).not.toHaveAttribute('data-disabled');

        await userEvent.click(newDeploymentMenuItem);

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
