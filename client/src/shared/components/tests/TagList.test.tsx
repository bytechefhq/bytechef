import {TooltipProvider} from '@/components/ui/tooltip';
import TagList from '@/shared/components/TagList';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

describe('TagList', () => {
    const renderTagList = (tags: Array<{id: number; name: string}> = [], readOnly = false) =>
        render(
            <TooltipProvider>
                <TagList
                    getRequest={vi.fn()}
                    id={1}
                    readOnly={readOnly}
                    remainingTags={[{id: 1, name: 'sales'}]}
                    tags={tags}
                    updateTagsMutation={{mutate: vi.fn()}}
                />
            </TooltipProvider>
        );

    it('hides the tags icon when there are no tags', () => {
        renderTagList();

        expect(screen.queryByLabelText('Tags')).not.toBeInTheDocument();
    });

    it('shows the tags icon when there are tags', () => {
        renderTagList([{id: 2, name: 'finance'}]);

        expect(screen.getByLabelText('Tags')).toBeInTheDocument();
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

    it('offers removing each tag by default', () => {
        renderTagList([
            {id: 2, name: 'finance'},
            {id: 3, name: 'support'},
        ]);

        expect(screen.getAllByRole('button', {name: 'Remove tag'})).toHaveLength(2);
    });

    it('shows the tags without add or remove controls when read-only', () => {
        renderTagList(
            [
                {id: 2, name: 'finance'},
                {id: 3, name: 'support'},
            ],
            true
        );

        expect(screen.getByText('finance')).toBeInTheDocument();
        expect(screen.getByText('support')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Remove tag'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add tag'})).not.toBeInTheDocument();
    });
});
