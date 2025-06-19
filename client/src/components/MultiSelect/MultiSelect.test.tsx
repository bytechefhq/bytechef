import {MultiSelect, MultiSelectOptionType} from '@/components/MultiSelect/MultiSelect';
import {fireEvent, mockScrollIntoView, render, screen, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

mockScrollIntoView();
windowResizeObserver();

const mockOnValueChange = vi.fn();

const mockOptions: MultiSelectOptionType[] = [
    {label: 'Option One', value: 'option-one'},
    {label: 'Option Two', value: 'option-two'},
    {label: 'Option Three', value: 'option-three'},
    {label: 'Option Four', value: 'option-four'},
];

interface RenderMultiSelectProps {
    defaultValue?: string[];
    disabled?: boolean;
    leadingIcon?: ReactNode;
    maxCount?: number;
    placeholder?: string;
    searchable?: boolean;
    showFooter?: boolean;
    value?: string[];
}

const renderMultiSelect = ({
    defaultValue = [],
    disabled = false,
    leadingIcon,
    maxCount = 3,
    placeholder = 'Select...',
    searchable = false,
    showFooter = false,
    value,
}: RenderMultiSelectProps) =>
    render(
        <MultiSelect
            defaultValue={defaultValue}
            disabled={disabled}
            leadingIcon={leadingIcon}
            maxCount={maxCount}
            onValueChange={mockOnValueChange}
            options={mockOptions}
            placeholder={placeholder}
            searchable={searchable}
            showFooter={showFooter}
            value={value}
        />
    );

describe('MultiSelect component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should show placeholder when no values are selected', () => {
        renderMultiSelect({});

        expect(screen.getByText('Select...')).toBeInTheDocument();
    });

    it('should show a custom placeholder', () => {
        renderMultiSelect({placeholder: 'Choose options...'});

        expect(screen.getByText('Choose options...')).toBeInTheDocument();
    });

    it('should show the selected values using defaultValue', () => {
        renderMultiSelect({defaultValue: ['option-one', 'option-two']});
        expect(screen.getByLabelText('Option One-selected')).toBeInTheDocument();
        expect(screen.getByLabelText('Option Two-selected')).toBeInTheDocument();
    });

    it('should show the selected values using value prop', async () => {
        const {rerender} = renderMultiSelect({value: ['option-three', 'option-four']});

        await waitFor(() => {
            expect(screen.getByLabelText('Option Three-selected')).toBeInTheDocument();
            expect(screen.getByLabelText('Option Four-selected')).toBeInTheDocument();
        });

        rerender(
            <MultiSelect onValueChange={mockOnValueChange} options={mockOptions} value={['option-one', 'option-two']} />
        );

        await waitFor(() => {
            expect(screen.getByLabelText('Option One-selected')).toBeInTheDocument();
            expect(screen.getByLabelText('Option Two-selected')).toBeInTheDocument();
        });
    });

    it('should clear all selections when X icon is clicked', async () => {
        renderMultiSelect({defaultValue: ['option-one', 'option-two']});

        const clearButton = screen.getByLabelText('clear-all');

        if (clearButton) {
            fireEvent.click(clearButton);

            expect(mockOnValueChange).toHaveBeenCalledWith([]);
        }
    });

    it('should remove a single selection when its X icon is clicked', async () => {
        renderMultiSelect({defaultValue: ['option-one', 'option-two']});

        const removeButtons = screen.getAllByLabelText('remove-option');

        if (removeButtons) {
            fireEvent.click(removeButtons[0]);

            expect(mockOnValueChange).toHaveBeenCalledWith(['option-two']);
        }
    });

    it('should show "+ X more" badge when selected options exceed maxCount', () => {
        renderMultiSelect({
            defaultValue: ['option-one', 'option-two', 'option-three', 'option-four'],
            maxCount: 2,
        });

        expect(screen.getByLabelText('Option One-selected')).toBeInTheDocument();
        expect(screen.getByLabelText('Option Two-selected')).toBeInTheDocument();
        expect(screen.getByText('+ 2 more')).toBeInTheDocument();

        expect(screen.queryByLabelText('Option Three-selected')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Option Four-selected')).not.toBeInTheDocument();
    });

    it('should clear extra options when "+ X more" badge X icon is clicked', async () => {
        renderMultiSelect({
            defaultValue: ['option-one', 'option-two', 'option-three', 'option-four'],
            maxCount: 2,
        });

        const clearExtraButton = screen.getByLabelText('clear-extra-options');

        if (clearExtraButton) {
            fireEvent.click(clearExtraButton as Element);

            expect(mockOnValueChange).toHaveBeenCalledWith(['option-one', 'option-two']);
        }
    });

    it('should open the dropdown when clicked', async () => {
        renderMultiSelect({});

        fireEvent.click(screen.getByText('Select...'));

        await waitFor(() => expect(screen.getByText('Select All')).toBeInTheDocument());
    });

    it('should select an option when clicked', async () => {
        renderMultiSelect({});

        fireEvent.click(screen.getByText('Select...'));

        await waitFor(() => {
            expect(screen.getByLabelText('Option Two')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByLabelText('Option Two'));

        expect(mockOnValueChange).toHaveBeenCalledWith(['option-two']);
    });

    it('should select all options when "Select All" is clicked', async () => {
        renderMultiSelect({});

        fireEvent.click(screen.getByLabelText('Select...'));

        await waitFor(() => {
            expect(screen.getByLabelText('Select All')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByLabelText('Select All'));

        expect(mockOnValueChange).toHaveBeenCalledWith(['option-one', 'option-two', 'option-three', 'option-four']);
    });

    it('should clear all selections when "Clear" button is clicked', async () => {
        renderMultiSelect({
            defaultValue: ['option-one', 'option-two'],
            showFooter: true,
        });

        // Open dropdown
        fireEvent.click(screen.getByText('Option One'));

        await waitFor(() => {
            expect(screen.getByText('Clear')).toBeInTheDocument();
        });

        // Click "Clear" button in footer
        fireEvent.click(screen.getByText('Clear'));

        // Should call onValueChange with empty array
        expect(mockOnValueChange).toHaveBeenCalledWith([]);
    });

    it('should clear all selections when clicking "Select All" if all options are already selected', async () => {
        renderMultiSelect({
            defaultValue: mockOptions.map((option) => option.value), // Select all options
        });

        fireEvent.click(screen.getByText('Option One'));

        await waitFor(() => {
            const selectAllCheckbox = screen.getByLabelText('Select All');

            expect(selectAllCheckbox.parentElement?.ariaSelected).toBe('true');
        });

        fireEvent.click(screen.getByLabelText('Select All'));

        expect(mockOnValueChange).toHaveBeenCalledWith([]);

        expect(screen.getByLabelText('Select All')).toBeInTheDocument();
    });

    it('should render the leadingIcon when provided', () => {
        const MockIcon = () => <div data-testid="mock-leading-icon">Icon</div>;

        const {rerender} = renderMultiSelect({
            leadingIcon: <MockIcon />,
        });

        expect(screen.getByTestId('mock-leading-icon')).toBeInTheDocument();

        rerender(<MultiSelect onValueChange={mockOnValueChange} options={mockOptions} />);

        expect(screen.queryByTestId('mock-leading-icon')).not.toBeInTheDocument();
    });

    it('should render search input when searchable prop is true', async () => {
        const {rerender} = renderMultiSelect({
            searchable: true,
        });

        fireEvent.click(screen.getByText('Select...'));

        await waitFor(() => {
            expect(screen.getByPlaceholderText('Search...')).toBeInTheDocument();
        });

        fireEvent.click(screen.getByText('Select...'));

        rerender(<MultiSelect onValueChange={mockOnValueChange} options={mockOptions} />);

        fireEvent.click(screen.getByText('Select...'));

        await waitFor(() => {
            expect(screen.queryByPlaceholderText('Search...')).not.toBeInTheDocument();
            expect(screen.getByLabelText('Select All')).toBeInTheDocument();
        });
    });
});
