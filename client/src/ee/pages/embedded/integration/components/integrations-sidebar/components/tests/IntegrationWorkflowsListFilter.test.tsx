import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationWorkflowsListFilter from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListFilter';
import {fireEvent, render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

const mockSetSortBy = vi.fn();

const mockSetSearchValue = vi.fn();

const renderIntegrationWorkflowsListFilter = (searchValue: string, sortBy: string) => {
    render(
        <TooltipProvider>
            <IntegrationWorkflowsListFilter
                searchValue={searchValue}
                setSearchValue={mockSetSearchValue}
                setSortBy={mockSetSortBy}
                sortBy={sortBy}
            />
        </TooltipProvider>
    );
};

it('should render the search input empty and sort dropdown closed by default', () => {
    renderIntegrationWorkflowsListFilter('', 'last-edited');

    expect(screen.getByPlaceholderText('Search workflows')).toBeInTheDocument();

    expect(screen.queryByLabelText('Sort by')).toHaveAttribute('aria-expanded', 'false');
});

it('should call setSearchValue with correct value when the search input value changes', () => {
    renderIntegrationWorkflowsListFilter('', 'last-edited');

    const searchInput = screen.getByPlaceholderText('Search workflows');

    fireEvent.change(searchInput, {target: {value: 'test'}});

    expect(mockSetSearchValue).toHaveBeenCalledWith('test');
});

it('should open the sort dropdown when the sort button is clicked', async () => {
    renderIntegrationWorkflowsListFilter('', 'last-edited');

    userEvent.click(screen.getByLabelText('Sort by'));

    await waitFor(() => {
        expect(screen.getByLabelText('Sort by')).toHaveAttribute('aria-expanded', 'true');
    });
});

it('should call setSortBy with correct value when a sort option is selected', async () => {
    renderIntegrationWorkflowsListFilter('', 'last-edited');

    await userEvent.click(screen.getByLabelText('Sort by'));

    await userEvent.click(screen.getByText('Date created'));

    await waitFor(() => {
        expect(mockSetSortBy).toHaveBeenCalledWith('date-created');
    });
});

it('should render with the design-system DropdownMenu', async () => {
    renderIntegrationWorkflowsListFilter('', 'last-edited');

    await userEvent.click(screen.getByLabelText('Sort by'));

    expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
    expect(screen.getByRole('menuitem', {name: 'Date created'})).toHaveClass('text-content-neutral-primary');
});
