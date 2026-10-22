import {TooltipProvider} from '@/components/ui/tooltip';
import WorkflowsSidebarListItem from '@/shared/components/workflow/WorkflowsSidebarListItem';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

const renderItem = (onClick = vi.fn()) => {
    render(
        <TooltipProvider>
            <ul>
                <WorkflowsSidebarListItem
                    active={false}
                    components={<span>components</span>}
                    editedTimeAgo="2 hours ago"
                    label="Review and Respond"
                    menu={<button type="button">Workflow menu</button>}
                    onClick={onClick}
                />
            </ul>
        </TooltipProvider>
    );

    return onClick;
};

describe('WorkflowsSidebarListItem', () => {
    it('opens the workflow on click', () => {
        const onClick = renderItem();

        fireEvent.click(screen.getByText('Review and Respond'));

        expect(onClick).toHaveBeenCalledTimes(1);
    });

    it.each(['Enter', ' '])('opens the workflow from the keyboard with %j', (key) => {
        const onClick = renderItem();

        fireEvent.keyDown(screen.getByRole('button', {name: /Review and Respond/}), {key});

        expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('does not open the workflow when the menu is used', () => {
        const onClick = renderItem();

        fireEvent.click(screen.getByRole('button', {name: 'Workflow menu'}));

        expect(onClick).not.toHaveBeenCalled();
    });

    it('shows when the workflow was edited', () => {
        renderItem();

        expect(screen.getByText('2 hours ago')).toBeInTheDocument();
    });
});
