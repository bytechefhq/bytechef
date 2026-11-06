import {TooltipProvider} from '@/components/ui/tooltip';
import TagList from '@/shared/components/TagList';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

describe('TagList', () => {
    const renderTagList = (tags: Array<{id: number; name: string}> = []) =>
        render(
            <TooltipProvider>
                <TagList
                    getRequest={vi.fn()}
                    id={1}
                    remainingTags={[{id: 1, name: 'sales'}]}
                    tags={tags}
                    updateTagsMutation={{mutate: vi.fn()}}
                />
            </TooltipProvider>
        );

    it('hides the "Tags:" label when there are no tags', () => {
        renderTagList();

        expect(screen.queryByText('Tags:')).not.toBeInTheDocument();
    });

    it('shows the "Tags:" label when there are tags', () => {
        renderTagList([{id: 2, name: 'finance'}]);

        expect(screen.getByText('Tags:')).toBeInTheDocument();
    });

    it('renders a labeled "Add tag" button', () => {
        renderTagList();

        expect(screen.getByRole('button', {name: 'Add tag'})).toBeInTheDocument();
    });

    it('opens the tag picker when "Add tag" is clicked', async () => {
        const user = userEvent.setup();

        renderTagList();

        await user.click(screen.getByRole('button', {name: 'Add tag'}));

        expect(screen.getByRole('combobox')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add tag'})).not.toBeInTheDocument();
    });
});
