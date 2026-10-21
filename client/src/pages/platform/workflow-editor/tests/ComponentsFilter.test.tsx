import {TooltipProvider} from '@/components/ui/tooltip';
import {
    fireEvent,
    render,
    resetAll,
    screen,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

import ComponentsFilter from '../components/filters/ComponentsFilter';

const mockComponentDefinitions = [
    {
        componentCategories: [{name: 'Category1'}, {name: 'Category2'}],
        name: 'Component1',
        version: 1,
    },
    {
        componentCategories: [{name: 'Category3'}],
        name: 'Component2',
        version: 1,
    },
    {
        componentCategories: [{name: 'Category1'}],
        name: 'Component3',
        version: 1,
    },
];

const mockFilteredCategories = [{label: 'Category1'}, {label: 'Category2'}, {label: 'Category3'}];

const mockSetSearchValue = vi.fn();
const mockToggleCategory = vi.fn();
const mockSetActiveView = vi.fn();
const mockDeselectAllCategories = vi.fn();

const renderComponentsFilter = (
    filterLabel: string,
    filterTooltip: string,
    customState = {},
    filteredCategories: {icon?: ReactNode; label: string}[] = mockFilteredCategories
) => {
    const mockFilterState = {
        activeView: 'all' as const,
        filteredCount: 0,
        searchValue: '',
        selectedCategories: [],
        ...customState,
    };

    render(
        <TooltipProvider>
            <ComponentsFilter
                componentDefinitions={mockComponentDefinitions}
                deselectAllCategories={mockDeselectAllCategories}
                filterConfig={{
                    label: filterLabel,
                    tooltip: filterTooltip,
                }}
                filterState={mockFilterState}
                filteredCategories={filteredCategories}
                filteredComponents={[]}
                setActiveView={mockSetActiveView}
                setSearchValue={mockSetSearchValue}
                toggleCategory={mockToggleCategory}
            />
        </TooltipProvider>
    );
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

it('Should render the ComponentsFilter component for actions with "All" button visible and showing the number of components, and "Filtered" button invisible', () => {
    renderComponentsFilter('actions', 'Filter actions by category');

    expect(screen.getByLabelText('All button')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByLabelText('Filtered button')).toHaveClass('invisible');
});

it('should open the actions dropdown and show the list of categories, but have the "Deselect" button hidden when the dropdown menu trigger is clicked', async () => {
    renderComponentsFilter('actions', 'Filter actions by category');

    const filterDropdownButton = screen.getByLabelText('Filter actions');

    expect(filterDropdownButton).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('Find category')).not.toBeInTheDocument();
    expect(screen.queryByText('Category1')).not.toBeInTheDocument();

    userEvent.click(filterDropdownButton);

    await waitFor(() => {
        expect(screen.getByPlaceholderText('Find category')).toBeInTheDocument();
        expect(screen.getByLabelText('Deselect button')).toHaveClass('hidden');
        expect(screen.getByText('Category1')).toBeInTheDocument();
        expect(screen.getByText('Category2')).toBeInTheDocument();
    });
});

it('should call "toggleCategory" with correct category name on click for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category');

    userEvent.click(screen.getByLabelText('Filter actions'));

    await waitFor(() => {
        userEvent.click(screen.getByText('Category1'));

        expect(mockToggleCategory).toHaveBeenCalledWith('Category1');
    });
});

it('should call "setSearchValue" with correct value when the search input value changes for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category');

    userEvent.click(screen.getByLabelText('Filter actions'));

    await waitFor(() => {
        const searchInput = screen.getByPlaceholderText('Find category');

        fireEvent.change(searchInput, {target: {value: 'test'}});
    });

    expect(mockSetSearchValue).toHaveBeenCalledWith('test');
});

it('should show the "X" icon when the search input has a value and call "setSearchValue" with empty string when the button is clicked for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {
        searchValue: 'test',
    });

    userEvent.click(screen.getByLabelText('Filter actions'));

    await waitFor(() => {
        expect(screen.getByLabelText('Clear search input')).toBeInTheDocument();

        userEvent.click(screen.getByLabelText('Clear search input'));

        expect(mockSetSearchValue).toHaveBeenCalledWith('');
    });
});

it('should call "setActiveView" with "filtered" when the "Filtered" button is clicked for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {
        activeView: 'all',
    });

    const filteredButton = screen.getByLabelText('Filtered button');

    userEvent.click(filteredButton);

    await waitFor(() => {
        expect(mockSetActiveView).toHaveBeenCalledWith('filtered');
    });
});

it('should call "setActiveView" with "all" when the "All" button is clicked for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {
        activeView: 'filtered',
    });

    const allButton = screen.getByLabelText('All button');

    userEvent.click(allButton);

    await waitFor(() => {
        expect(mockSetActiveView).toHaveBeenCalledWith('all');
    });
});

it('Should show "Filtered" button as visible with correct "filteredCount" and "Deselect" button as visible when there are selected categories for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {
        filteredCount: 2,
        selectedCategories: ['Category1'],
    });

    expect(screen.getByLabelText('Filtered button')).toHaveClass('visible');

    expect(screen.getByText('2')).toBeInTheDocument();

    userEvent.click(screen.getByLabelText('Filter actions'));

    await waitFor(() => {
        expect(screen.getByLabelText('Deselect button')).not.toHaveClass('hidden');
    });
});

it('should call "deselectAllCategories" when "Deselect" button is clicked for actions', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {
        filteredCount: 2,
        selectedCategories: ['Category1'],
    });

    userEvent.click(screen.getByLabelText('Filter actions'));

    await waitFor(() => {
        expect(screen.getByLabelText('Deselect button')).not.toHaveClass('hidden');

        userEvent.click(screen.getByLabelText('Deselect button'));

        expect(mockDeselectAllCategories).toHaveBeenCalled();
    });
});

it('Should render the ComponentsFilter component for triggers with "All" button visible and showing the number of components, and "Filtered" button invisible', () => {
    renderComponentsFilter('triggers', 'Filter triggers by category');

    expect(screen.getByLabelText('All button')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByLabelText('Filtered button')).toHaveClass('invisible');
});

it('should open the triggers dropdown and show the list of categories, but have the "Deselect" button hidden when the dropdown menu trigger is clicked', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category');

    const filterDropdownButton = screen.getByLabelText('Filter triggers');

    expect(filterDropdownButton).toBeInTheDocument();
    expect(screen.queryByPlaceholderText('Find category')).not.toBeInTheDocument();
    expect(screen.queryByText('Category1')).not.toBeInTheDocument();

    userEvent.click(filterDropdownButton);

    await waitFor(() => {
        expect(screen.getByPlaceholderText('Find category')).toBeInTheDocument();
        expect(screen.getByLabelText('Deselect button')).toHaveClass('hidden');
        expect(screen.getByText('Category1')).toBeInTheDocument();
        expect(screen.getByText('Category2')).toBeInTheDocument();
    });
});

it('should call "toggleCategory" with correct category name on click for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category');

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        userEvent.click(screen.getByText('Category1'));

        expect(mockToggleCategory).toHaveBeenCalledWith('Category1');
    });
});

it('should call "setSearchValue" with correct value when the search input value changes for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category');

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        const searchInput = screen.getByPlaceholderText('Find category');

        fireEvent.change(searchInput, {target: {value: 'test'}});
    });

    expect(mockSetSearchValue).toHaveBeenCalledWith('test');
});

it('should show the "X" icon when the search input has a value and call "setSearchValue" with empty string when the button is clicked for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category', {
        searchValue: 'test',
    });

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        expect(screen.getByLabelText('Clear search input')).toBeInTheDocument();

        userEvent.click(screen.getByLabelText('Clear search input'));

        expect(mockSetSearchValue).toHaveBeenCalledWith('');
    });
});

it('should call "setActiveView" with "filtered" when the "Filtered" button is clicked for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category', {
        activeView: 'all',
    });

    const filteredButton = screen.getByLabelText('Filtered button');

    userEvent.click(filteredButton);

    await waitFor(() => {
        expect(mockSetActiveView).toHaveBeenCalledWith('filtered');
    });
});

it('should call "setActiveView" with "all" when the "All" button is clicked for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category', {
        activeView: 'filtered',
    });

    const allButton = screen.getByLabelText('All button');

    userEvent.click(allButton);

    await waitFor(() => {
        expect(mockSetActiveView).toHaveBeenCalledWith('all');
    });
});

it('Should show "Filtered" button as visible with correct "filteredCount" and "Deselect" button as visible when there are selected categories for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category', {
        filteredCount: 2,
        selectedCategories: ['Category1'],
    });

    expect(screen.getByLabelText('Filtered button')).toHaveClass('visible');

    expect(screen.getByText('2')).toBeInTheDocument();

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        expect(screen.getByLabelText('Deselect button')).not.toHaveClass('hidden');
    });
});

it('should call "deselectAllCategories" when "Deselect" button is clicked for triggers', async () => {
    renderComponentsFilter('triggers', 'Filter triggers by category', {
        filteredCount: 2,
        selectedCategories: ['Category1'],
    });

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        expect(screen.getByLabelText('Deselect button')).not.toHaveClass('hidden');

        userEvent.click(screen.getByLabelText('Deselect button'));

        expect(mockDeselectAllCategories).toHaveBeenCalled();
    });
});

it('should render the categories with the design-system DropdownMenu', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {selectedCategories: ['Category2']});

    await userEvent.click(screen.getByLabelText('Filter actions'));

    expect(screen.getByRole('menu')).toHaveClass('bg-surface-neutral-primary');
    expect(screen.getAllByRole('menuitemcheckbox').map((menuItem) => menuItem.textContent)).toEqual([
        'Category1',
        'Category2',
        'Category3',
    ]);
    expect(screen.getByRole('menuitemcheckbox', {name: 'Category1'})).toHaveAttribute('aria-checked', 'false');
    expect(screen.getByRole('menuitemcheckbox', {name: 'Category2'})).toHaveAttribute('aria-checked', 'true');
});

it('should toggle a category and keep the menu open when clicking its row', async () => {
    renderComponentsFilter('actions', 'Filter actions by category');

    await userEvent.click(screen.getByLabelText('Filter actions'));

    await userEvent.click(screen.getByRole('menuitemcheckbox', {name: 'Category3'}));

    expect(mockToggleCategory).toHaveBeenCalledTimes(1);
    expect(mockToggleCategory).toHaveBeenCalledWith('Category3');
    expect(screen.getByRole('menu')).toBeInTheDocument();
});

it('should show the category icon, falling back to the bot icon, in each row', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {}, [
        {icon: <span data-testid="category-icon" />, label: 'Category1'},
        {label: 'Category2'},
    ]);

    await userEvent.click(screen.getByLabelText('Filter actions'));

    const categoryWithIconRow = screen.getByRole('menuitemcheckbox', {name: 'Category1'});
    const categoryWithoutIconRow = screen.getByRole('menuitemcheckbox', {name: 'Category2'});

    expect(within(categoryWithIconRow).getByTestId('category-icon')).toBeInTheDocument();
    expect(categoryWithIconRow.querySelector('svg.lucide-bot')).not.toBeInTheDocument();
    expect(categoryWithoutIconRow.querySelector('svg.lucide-bot')).toBeInTheDocument();
});

it('should show a message instead of rows when no category matches', async () => {
    renderComponentsFilter('actions', 'Filter actions by category', {}, []);

    await userEvent.click(screen.getByLabelText('Filter actions'));

    expect(screen.getByText('No categories found')).toBeInTheDocument();
    expect(screen.queryByRole('menuitemcheckbox')).not.toBeInTheDocument();
});

it('should keep the search input focused while hovering over a category row', async () => {
    renderComponentsFilter('actions', 'Filter actions by category');

    await userEvent.click(screen.getByLabelText('Filter actions'));

    const searchInput = screen.getByPlaceholderText('Find category');

    searchInput.focus();

    const categoryRow = screen.getByRole('menuitemcheckbox', {name: 'Category1'});

    await userEvent.hover(categoryRow);

    expect(searchInput).toHaveFocus();

    await userEvent.unhover(categoryRow);

    expect(searchInput).toHaveFocus();
});
