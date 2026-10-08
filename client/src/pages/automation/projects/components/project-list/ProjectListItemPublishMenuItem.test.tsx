import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/DropdownMenu/DropdownMenu';
import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import ProjectListItemPublishMenuItem from './ProjectListItemPublishMenuItem';

const renderPublishMenuItem = (hasWorkflows: boolean, onClick = vi.fn()) => {
    render(
        <TooltipProvider>
            <DropdownMenu open>
                <DropdownMenuTrigger>More Project Actions</DropdownMenuTrigger>

                <DropdownMenuContent>
                    <ProjectListItemPublishMenuItem hasWorkflows={hasWorkflows} onClick={onClick} />
                </DropdownMenuContent>
            </DropdownMenu>
        </TooltipProvider>
    );

    return onClick;
};

describe('ProjectListItemPublishMenuItem', () => {
    it('should disable publishing and explain why when the project has no workflows', async () => {
        const onClick = renderPublishMenuItem(false);

        const publishMenuItem = screen.getByRole('menuitem', {name: 'Publish Project'});

        expect(publishMenuItem).toHaveAttribute('data-disabled');

        await userEvent.hover(publishMenuItem.parentElement!);

        expect(await screen.findByRole('tooltip')).toHaveTextContent('Add a workflow before publishing the project.');

        await userEvent.click(publishMenuItem, {pointerEventsCheck: 0});

        expect(onClick).not.toHaveBeenCalled();
    });

    it('should publish when the project has workflows', async () => {
        const onClick = renderPublishMenuItem(true);

        const publishMenuItem = screen.getByRole('menuitem', {name: 'Publish Project'});

        expect(publishMenuItem).not.toHaveAttribute('data-disabled');

        await userEvent.click(publishMenuItem);

        expect(onClick).toHaveBeenCalledTimes(1);
    });
});
