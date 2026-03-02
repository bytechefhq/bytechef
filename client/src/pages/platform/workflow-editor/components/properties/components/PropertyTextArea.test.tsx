import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import PropertyTextArea from './PropertyTextArea';

describe('PropertyTextArea', () => {
    it('renders with label and placeholder', () => {
        render(
            <PropertyTextArea
                error={false}
                errorMessage=""
                label="Description"
                name="description"
                placeholder="Enter description"
            />
        );

        expect(screen.getByText('Description')).toBeInTheDocument();
        expect(screen.getByPlaceholderText('Enter description')).toBeInTheDocument();
    });

    it('renders trailingAction when provided', () => {
        render(
            <PropertyTextArea
                error={false}
                errorMessage=""
                label="Notes"
                name="notes"
                trailingAction={<button data-testid="trailing-action">Toggle</button>}
            />
        );

        expect(screen.getByTestId('trailing-action')).toBeInTheDocument();
    });

    it('renders deletePropertyButton in the label area', () => {
        render(
            <PropertyTextArea
                deletePropertyButton={<button data-testid="delete-button">Delete</button>}
                error={false}
                errorMessage=""
                label="Notes"
                name="notes"
            />
        );

        expect(screen.getByTestId('delete-button')).toBeInTheDocument();
    });

    it('shows required mark when required is true', () => {
        render(
            <PropertyTextArea
                error={false}
                errorMessage=""
                label="Required Field"
                name="required"
                required
            />
        );

        expect(screen.getByText('*')).toBeInTheDocument();
    });

    it('renders error state with error icon', () => {
        render(
            <PropertyTextArea
                error={true}
                errorMessage="Something went wrong"
                label="Broken"
                name="broken"
            />
        );

        // Error icon should be present (TriangleAlertIcon)
        expect(screen.getByRole('textbox')).toHaveClass('border-rose-300');
    });
});
