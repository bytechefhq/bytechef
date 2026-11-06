import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import CreatableSelect from './CreatableSelect';

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

describe('CreatableSelect', () => {
    it('should tell the user they can type their own value', () => {
        render(<CreatableSelect name="value" options={[]} />);

        expect(screen.getByText('Select or type to create...')).toBeInTheDocument();
    });

    it('should let the caller override the placeholder', () => {
        render(<CreatableSelect name="value" options={[]} placeholder="Custom placeholder" />);

        expect(screen.getByText('Custom placeholder')).toBeInTheDocument();
        expect(screen.queryByText('Select or type to create...')).not.toBeInTheDocument();
    });

    it('should invite creating a value when there are no predefined options', async () => {
        render(<CreatableSelect name="value" options={[]} />);

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByText('Type to create a new value')).toBeInTheDocument();
    });

    it('should offer to create the typed value', async () => {
        render(<CreatableSelect name="value" options={[{label: 'billing', value: 'billing'}]} />);

        await userEvent.type(screen.getByRole('combobox'), 'marketing');

        expect(screen.getByText('Create "marketing"')).toBeInTheDocument();
    });

    it('should render the menu into the dialog content so the scrolling dialog body does not clip it', async () => {
        render(
            <div data-slot="dialog-content" data-testid="dialog-content">
                <div data-testid="dialog-body">
                    <CreatableSelect name="value" options={[{label: 'billing', value: 'billing'}]} />
                </div>
            </div>
        );

        await userEvent.click(screen.getByRole('combobox'));

        const option = screen.getByText('billing');

        expect(screen.getByTestId('dialog-body')).not.toContainElement(option);
        expect(screen.getByTestId('dialog-content')).toContainElement(option);
    });

    it('should keep the menu next to the control outside a dialog', async () => {
        render(
            <div data-testid="container">
                <CreatableSelect name="value" options={[{label: 'billing', value: 'billing'}]} />
            </div>
        );

        await userEvent.click(screen.getByRole('combobox'));

        expect(screen.getByTestId('container')).toContainElement(screen.getByText('billing'));
    });
});
