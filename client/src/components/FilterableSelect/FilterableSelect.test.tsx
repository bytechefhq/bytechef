import FilterableSelect from '@/components/FilterableSelect/FilterableSelect';
import {TooltipProvider} from '@/components/ui/tooltip';
import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ComponentProps} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

mockScrollIntoView();
windowResizeObserver();

const mockOnValueChange = vi.fn();

const items = [
    {label: 'Brokers', value: '1'},
    {label: 'Car Dealer', value: '2'},
    {label: 'Control Check', value: '3'},
];

const pinnedItems = [
    {label: 'Current project', value: '1'},
    {label: 'All projects', value: '0'},
];

const renderFilterableSelect = (props: Partial<ComponentProps<typeof FilterableSelect>> = {}) =>
    render(
        <TooltipProvider>
            <FilterableSelect
                ariaLabel="Select project"
                emptyMessage="No projects found."
                items={items}
                onValueChange={mockOnValueChange}
                pinnedItems={pinnedItems}
                searchPlaceholder="Search projects..."
                triggerLabel="Current project"
                value="1"
                {...props}
            />
        </TooltipProvider>
    );

describe('FilterableSelect', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('shows the trigger label and keeps the list closed', () => {
        renderFilterableSelect();

        expect(screen.getByRole('combobox', {name: 'Select project'})).toHaveTextContent('Current project');
        expect(screen.queryByPlaceholderText('Search projects...')).not.toBeInTheDocument();
    });

    it('lists pinned items and items when opened', async () => {
        renderFilterableSelect();

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));

        expect(screen.getByPlaceholderText('Search projects...')).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'All projects'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Brokers'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Car Dealer'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Control Check'})).toBeInTheDocument();
    });

    it('narrows items and pinned items by label, ignoring case, in the caller order', async () => {
        renderFilterableSelect();

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
        await userEvent.type(screen.getByPlaceholderText('Search projects...'), 'C');

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual([
            'Current project',
            'All projects',
            'Car Dealer',
            'Control Check',
        ]);

        await userEvent.type(screen.getByPlaceholderText('Search projects...'), 'ar');

        expect(screen.getAllByRole('option').map((option) => option.textContent)).toEqual(['Car Dealer']);
    });

    it('shows the empty message when nothing matches', async () => {
        renderFilterableSelect();

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
        await userEvent.type(screen.getByPlaceholderText('Search projects...'), 'zzz');

        expect(screen.queryAllByRole('option')).toHaveLength(0);
        expect(screen.getByText('No projects found.')).toBeInTheDocument();
    });

    it('calls onValueChange with the selected filtered item and clears the search on reopen', async () => {
        renderFilterableSelect();

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
        await userEvent.type(screen.getByPlaceholderText('Search projects...'), 'control');
        await userEvent.click(screen.getByRole('option', {name: 'Control Check'}));

        expect(mockOnValueChange).toHaveBeenCalledWith('3');
        expect(screen.queryByPlaceholderText('Search projects...')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));

        expect(screen.getByPlaceholderText('Search projects...')).toHaveValue('');
    });

    it('selects a pinned item by its own value', async () => {
        renderFilterableSelect();

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));
        await userEvent.click(screen.getByRole('option', {name: 'All projects'}));

        expect(mockOnValueChange).toHaveBeenCalledWith('0');
    });

    it('marks only the rows carrying the current value as selected', async () => {
        renderFilterableSelect({value: '2'});

        await userEvent.click(screen.getByRole('combobox', {name: 'Select project'}));

        expect(screen.getByRole('option', {name: 'Car Dealer'}).querySelector('svg')).not.toBeNull();
        expect(screen.getByRole('option', {name: 'Brokers'}).querySelector('svg')).toBeNull();
    });
});
