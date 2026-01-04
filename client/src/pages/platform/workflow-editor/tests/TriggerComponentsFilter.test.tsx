import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

import TriggerComponentsFilter from '../components/filters/TriggerComponentsFilter';

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

const renderTriggerComponentsFilter = (customState = {}) => {
    const mockFilterState = {
        activeView: 'all' as const,
        filteredCount: 0,
        savedCategories: [],
        searchValue: '',
        selectedCategories: [],
        ...customState,
    };

    render(
        <TooltipProvider>
            <TriggerComponentsFilter
                deselectAllCategories={mockDeselectAllCategories}
                filterState={mockFilterState}
                filteredCategories={mockFilteredCategories}
                filteredComponents={[]}
                setActiveView={mockSetActiveView}
                setSearchValue={mockSetSearchValue}
                toggleCategory={mockToggleCategory}
                triggerComponentDefinitions={mockComponentDefinitions}
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

it('Should render the TriggerComponentsFilter component with "All" button visible and showing the number of components, and "Filtered" button invisible', () => {
    renderTriggerComponentsFilter();

    expect(screen.getByLabelText('All button')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByLabelText('Filtered button')).toHaveClass('invisible');
});

it('should open the dropdown and show the list of categories, but have the "Deselect" button hidden when the dropdown menu trigger is clicked', async () => {
    renderTriggerComponentsFilter();

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

it('should call "toggleCategory" with correct category name on click', async () => {
    renderTriggerComponentsFilter();

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        userEvent.click(screen.getByText('Category1'));

        expect(mockToggleCategory).toHaveBeenCalledWith('Category1');
    });
});

it('should call "setSearchValue" with correct value when the search input value changes', async () => {
    renderTriggerComponentsFilter();

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        const searchInput = screen.getByPlaceholderText('Find category');

        fireEvent.change(searchInput, {target: {value: 'test'}});
    });

    expect(mockSetSearchValue).toHaveBeenCalledWith('test');
});

it('should show the "X" icon when the search input has a value and call "setSearchValue" with empty string when the button is clicked', async () => {
    renderTriggerComponentsFilter({
        searchValue: 'test',
    });

    userEvent.click(screen.getByLabelText('Filter triggers'));

    await waitFor(() => {
        expect(screen.getByLabelText('Clear search input')).toBeInTheDocument();

        userEvent.click(screen.getByLabelText('Clear search input'));

        expect(mockSetSearchValue).toHaveBeenCalledWith('');
    });
});

it('should call "setActiveView" with "filtered" when the "Filtered" button is clicked', async () => {
    renderTriggerComponentsFilter({
        activeView: 'all',
    });

    const filteredButton = screen.getByLabelText('Filtered button');

    userEvent.click(filteredButton);

    await waitFor(() => {
        expect(mockSetActiveView).toHaveBeenCalledWith('filtered');
    });
});

it('should call "setActiveView" with "all" when the "All" button is clicked', async () => {
    renderTriggerComponentsFilter({
        activeView: 'filtered',
    });

    const allButton = screen.getByLabelText('All button');

    userEvent.click(allButton);

    await waitFor(() => {
        expect(mockSetActiveView).toHaveBeenCalledWith('all');
    });
});

it('Should show "Filtered" button as visible with correct "filteredCount" and "Deselect" button as visible when there are selected categories', async () => {
    renderTriggerComponentsFilter({
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

it('should call "deselectAllCategories" when "Deselect" button is clicked', async () => {
    renderTriggerComponentsFilter({
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
