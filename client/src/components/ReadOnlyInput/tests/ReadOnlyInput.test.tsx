import {render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import ReadOnlyInput from '../ReadOnlyInput';

describe('ReadOnlyInput', () => {
    it('shows the label and the text', () => {
        render(<ReadOnlyInput label="Component" text="Airtable" />);

        expect(screen.getByText('Component')).toBeInTheDocument();
        expect(screen.getByText('Airtable')).toBeInTheDocument();
    });

    it('applies the label class name to the label', () => {
        render(<ReadOnlyInput label="Component" labelClassName="leading-normal" text="Airtable" />);

        expect(screen.getByText('Component')).toHaveClass('leading-normal');
    });
});
