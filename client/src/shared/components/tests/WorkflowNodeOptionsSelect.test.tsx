import WorkflowNodeOptionsSelect from '@/shared/components/WorkflowNodeOptionsSelect';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

describe('WorkflowNodeOptionsSelect', () => {
    it('shows a loading placeholder while the options load', () => {
        render(<WorkflowNodeOptionsSelect isLoading onChange={vi.fn()} />);

        expect(screen.getByRole('option', {name: 'Loading...'})).toBeDisabled();
    });

    it('lists the options by label and reports the selected value', async () => {
        const onChange = vi.fn();

        render(
            <WorkflowNodeOptionsSelect
                isLoading={false}
                onChange={onChange}
                options={[{label: 'General', value: 'C1'}, {value: 'C2'}]}
            />
        );

        expect(screen.getByRole('option', {name: 'Select...'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'C2'})).toBeInTheDocument();

        await userEvent.selectOptions(screen.getByRole('combobox'), 'General');

        expect(onChange).toHaveBeenCalledWith('C1');
    });
});
