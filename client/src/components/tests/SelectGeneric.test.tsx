import {Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {expect, it, vi} from 'vitest';

const mockOnValueChange = vi.fn();

const renderSelect = (value?: string) => {
    render(
        <Select onValueChange={mockOnValueChange} value={value}>
            <SelectTrigger aria-label="Select option">
                <SelectValue placeholder="Select an option" />
            </SelectTrigger>

            <SelectContent>
                <SelectGroup>
                    <SelectItem value="option-one">Option One</SelectItem>

                    <SelectItem value="option-two">Option Two</SelectItem>

                    <SelectItem value="option-three">Option Three</SelectItem>

                    <SelectItem value="option-four">Option Four</SelectItem>
                </SelectGroup>
            </SelectContent>
        </Select>
    );
};

it('should show placeholder when no value is selected', () => {
    renderSelect();

    expect(screen.getByText('Select an option')).toBeInTheDocument();

    expect(screen.queryByText('Option One')).not.toBeInTheDocument();
});

it('should show the selected value', () => {
    renderSelect('option-one');

    expect(screen.getByText('Option One')).toBeInTheDocument();

    expect(screen.queryByText('Select an option')).not.toBeInTheDocument();
});

it('should open the select dropdown when the select trigger is clicked', () => {
    renderSelect();

    expect(screen.getByText('Select an option')).toBeInTheDocument();

    expect(screen.getByLabelText('Select option')).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(screen.getByLabelText('Select option'));

    expect(screen.getByLabelText('Select option')).toHaveAttribute('aria-expanded', 'true');
});

it('should show the list of options in the select dropdown', () => {
    renderSelect();

    expect(screen.queryByText('Option Two')).not.toBeInTheDocument();

    expect(screen.queryByText('Option Three')).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Select option'));

    expect(screen.getByText('Option Two')).toBeInTheDocument();

    expect(screen.getByText('Option Three')).toBeInTheDocument();
});

it('should call onValueChange with correct value when an item is selected', () => {
    renderSelect();

    fireEvent.click(screen.getByLabelText('Select option'));

    fireEvent.click(screen.getByText('Option Four'));

    expect(mockOnValueChange).toHaveBeenCalledWith('option-four');
});

it('should show the selected option as checked in the select dropdown', () => {
    renderSelect('option-three');

    fireEvent.click(screen.getByLabelText('Select option'));

    expect(screen.getByLabelText('Option One')).not.toHaveAttribute('data-state', 'checked');

    expect(screen.getByLabelText('Option Three')).toHaveAttribute('data-state', 'checked');
});

it('should close the select dropdown when an item is selected', () => {
    renderSelect();

    fireEvent.click(screen.getByLabelText('Select option'));

    fireEvent.click(screen.getByText('Option Four'));

    expect(screen.getByLabelText('Select option')).toHaveAttribute('aria-expanded', 'false');
});
